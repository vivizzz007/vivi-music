#!/usr/bin/env python3
import argparse
import gzip
import logging
import os
import re
import sys
import tempfile
import urllib.request
import xml.etree.ElementTree as ET
import math


def parse_length(value):
    if value is None:
        return None
    match = re.search(r"[-+]?[0-9]*\.?[0-9]+", value)
    if not match:
        return None
    return float(match.group(0))


def parse_viewbox(viewbox, width, height):
    if viewbox:
        parts = [float(p) for p in viewbox.replace(",", " ").split()]
        if len(parts) != 4:
            raise ValueError(f"Invalid viewBox: {viewbox}")
        return parts[0], parts[1], parts[2], parts[3]
    if width is None or height is None:
        raise ValueError("Missing viewBox and width/height")
    return 0.0, 0.0, float(width), float(height)


def parse_svg(path):
    tree = ET.parse(path)
    root = tree.getroot()

    width = parse_length(root.get("width"))
    height = parse_length(root.get("height"))
    min_x, min_y, vb_width, vb_height = parse_viewbox(
        root.get("viewBox"), width, height
    )

    root_fill = root.get("fill")
    paths = []
    for elem in root.findall(".//{*}path"):
        d = elem.get("d")
        if not d:
            continue
        fill = elem.get("fill") or root_fill or "#000000"
        if fill == "none" or fill == "currentColor":
            fill = "#000000"
        bbox = path_bbox(d)
        paths.append({"d": d, "fill": fill, "bbox": bbox})

    if not paths:
        raise ValueError(f"No <path> elements found in {path}")

    data = {
        "width": width,
        "height": height,
        "min_x": min_x,
        "min_y": min_y,
        "vb_width": vb_width,
        "vb_height": vb_height,
        "paths": paths,
    }
    logging.debug(
        "Parsed %s: viewBox=(%s %s %s %s), paths=%s",
        path,
        fmt_number(min_x),
        fmt_number(min_y),
        fmt_number(vb_width),
        fmt_number(vb_height),
        len(paths),
    )
    return data


def bbox_union(bbox, other):
    if other is None:
        return bbox
    if bbox is None:
        return other
    return (
        min(bbox[0], other[0]),
        min(bbox[1], other[1]),
        max(bbox[2], other[2]),
        max(bbox[3], other[3]),
    )


def bbox_translate(bbox, dx, dy):
    if bbox is None:
        return None
    return (bbox[0] + dx, bbox[1] + dy, bbox[2] + dx, bbox[3] + dy)


def bbox_scale(bbox, scale):
    if bbox is None:
        return None
    return (bbox[0] * scale, bbox[1] * scale, bbox[2] * scale, bbox[3] * scale)


def bbox_distance(a, b):
    if a is None or b is None:
        return float("inf")
    dx = max(0.0, max(b[0] - a[2], a[0] - b[2]))
    dy = max(0.0, max(b[1] - a[3], a[1] - b[3]))
    return math.hypot(dx, dy)


def path_bbox(path_data):
    tokens = re.findall(
        r"[MmLlHhVvCcSsQqTtAaZz]|[-+]?(?:\d*\.\d+|\d+)(?:[eE][-+]?\d+)?",
        path_data,
    )
    if not tokens:
        return None

    index = 0
    cmd = None
    start_x = 0.0
    start_y = 0.0
    x = 0.0
    y = 0.0
    last_cubic = None
    last_quad = None
    bbox = None

    def read_number():
        nonlocal index
        if index >= len(tokens):
            raise ValueError("Unexpected end of path data")
        value = float(tokens[index])
        index += 1
        return value

    def add_point(px, py):
        nonlocal bbox
        bbox = bbox_union(bbox, (px, py, px, py))

    def cubic_point(p0, p1, p2, p3, t):
        mt = 1.0 - t
        return (
            mt * mt * mt * p0
            + 3 * mt * mt * t * p1
            + 3 * mt * t * t * p2
            + t * t * t * p3
        )

    def quadratic_point(p0, p1, p2, t):
        mt = 1.0 - t
        return mt * mt * p0 + 2 * mt * t * p1 + t * t * p2

    def cubic_extrema(p0, p1, p2, p3):
        a = -p0 + 3 * p1 - 3 * p2 + p3
        b = 2 * (p0 - 2 * p1 + p2)
        c = p1 - p0
        ts = []
        if abs(a) < 1e-8:
            if abs(b) > 1e-8:
                t = -c / b
                if 0 < t < 1:
                    ts.append(t)
        else:
            disc = b * b - 4 * a * c
            if disc >= 0:
                root = math.sqrt(disc)
                t1 = (-b + root) / (2 * a)
                t2 = (-b - root) / (2 * a)
                if 0 < t1 < 1:
                    ts.append(t1)
                if 0 < t2 < 1:
                    ts.append(t2)
        return ts

    def quadratic_extrema(p0, p1, p2):
        denom = p0 - 2 * p1 + p2
        if abs(denom) < 1e-8:
            return []
        t = (p0 - p1) / denom
        if 0 < t < 1:
            return [t]
        return []

    def cubic_bbox(x0, y0, x1, y1, x2, y2, x3, y3):
        nonlocal bbox
        xs = [x0, x3]
        ys = [y0, y3]
        for t in cubic_extrema(x0, x1, x2, x3):
            xs.append(cubic_point(x0, x1, x2, x3, t))
        for t in cubic_extrema(y0, y1, y2, y3):
            ys.append(cubic_point(y0, y1, y2, y3, t))
        bbox = bbox_union(bbox, (min(xs), min(ys), max(xs), max(ys)))

    def quadratic_bbox(x0, y0, x1, y1, x2, y2):
        nonlocal bbox
        xs = [x0, x2]
        ys = [y0, y2]
        for t in quadratic_extrema(x0, x1, x2):
            xs.append(quadratic_point(x0, x1, x2, t))
        for t in quadratic_extrema(y0, y1, y2):
            ys.append(quadratic_point(y0, y1, y2, t))
        bbox = bbox_union(bbox, (min(xs), min(ys), max(xs), max(ys)))

    def arc_bbox(x0, y0, rx, ry, phi, large_arc, sweep, x1, y1):
        nonlocal bbox
        if rx == 0 or ry == 0:
            bbox = bbox_union(
                bbox, (min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1))
            )
            return
        phi_rad = math.radians(phi % 360.0)
        cos_phi = math.cos(phi_rad)
        sin_phi = math.sin(phi_rad)

        dx = (x0 - x1) / 2.0
        dy = (y0 - y1) / 2.0
        x1p = cos_phi * dx + sin_phi * dy
        y1p = -sin_phi * dx + cos_phi * dy

        rx = abs(rx)
        ry = abs(ry)
        lam = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry)
        if lam > 1:
            scale = math.sqrt(lam)
            rx *= scale
            ry *= scale

        sign = -1.0 if large_arc == sweep else 1.0
        num = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p
        den = rx * rx * y1p * y1p + ry * ry * x1p * x1p
        coef = 0.0
        if den != 0:
            coef = sign * math.sqrt(max(0.0, num / den))
        cxp = coef * (rx * y1p / ry)
        cyp = coef * (-ry * x1p / rx)

        cx = cos_phi * cxp - sin_phi * cyp + (x0 + x1) / 2.0
        cy = sin_phi * cxp + cos_phi * cyp + (y0 + y1) / 2.0

        def angle(u, v):
            return math.atan2(u[0] * v[1] - u[1] * v[0], u[0] * v[0] + u[1] * v[1])

        v1 = ((x1p - cxp) / rx, (y1p - cyp) / ry)
        v2 = ((-x1p - cxp) / rx, (-y1p - cyp) / ry)
        theta1 = angle((1.0, 0.0), v1)
        delta = angle(v1, v2)
        if not sweep and delta > 0:
            delta -= 2 * math.pi
        if sweep and delta < 0:
            delta += 2 * math.pi

        steps = 20
        for i in range(steps + 1):
            t = i / steps
            theta = theta1 + delta * t
            cos_t = math.cos(theta)
            sin_t = math.sin(theta)
            px = cx + rx * cos_phi * cos_t - ry * sin_phi * sin_t
            py = cy + rx * sin_phi * cos_t + ry * cos_phi * sin_t
            add_point(px, py)

    while index < len(tokens):
        token = tokens[index]
        if re.match(r"[A-Za-z]", token):
            cmd = token
            index += 1
        elif cmd is None:
            raise ValueError("Path data missing command")

        if cmd in "Mm":
            x1 = read_number()
            y1 = read_number()
            if cmd == "m":
                x1 += x
                y1 += y
            x = x1
            y = y1
            start_x = x
            start_y = y
            add_point(x, y)
            cmd = "L" if cmd == "M" else "l"
            last_cubic = None
            last_quad = None
        elif cmd in "Zz":
            add_point(start_x, start_y)
            x = start_x
            y = start_y
            last_cubic = None
            last_quad = None
        elif cmd in "Ll":
            x1 = read_number()
            y1 = read_number()
            if cmd == "l":
                x1 += x
                y1 += y
            bbox = bbox_union(bbox, (min(x, x1), min(y, y1), max(x, x1), max(y, y1)))
            x = x1
            y = y1
            last_cubic = None
            last_quad = None
        elif cmd in "Hh":
            x1 = read_number()
            if cmd == "h":
                x1 += x
            bbox = bbox_union(bbox, (min(x, x1), y, max(x, x1), y))
            x = x1
            last_cubic = None
            last_quad = None
        elif cmd in "Vv":
            y1 = read_number()
            if cmd == "v":
                y1 += y
            bbox = bbox_union(bbox, (x, min(y, y1), x, max(y, y1)))
            y = y1
            last_cubic = None
            last_quad = None
        elif cmd in "Cc":
            x1 = read_number()
            y1 = read_number()
            x2 = read_number()
            y2 = read_number()
            x3 = read_number()
            y3 = read_number()
            if cmd == "c":
                x1 += x
                y1 += y
                x2 += x
                y2 += y
                x3 += x
                y3 += y
            cubic_bbox(x, y, x1, y1, x2, y2, x3, y3)
            x = x3
            y = y3
            last_cubic = (x2, y2)
            last_quad = None
        elif cmd in "Ss":
            x2 = read_number()
            y2 = read_number()
            x3 = read_number()
            y3 = read_number()
            if last_cubic is not None:
                x1 = 2 * x - last_cubic[0]
                y1 = 2 * y - last_cubic[1]
            else:
                x1 = x
                y1 = y
            if cmd == "s":
                x2 += x
                y2 += y
                x3 += x
                y3 += y
            cubic_bbox(x, y, x1, y1, x2, y2, x3, y3)
            x = x3
            y = y3
            last_cubic = (x2, y2)
            last_quad = None
        elif cmd in "Qq":
            x1 = read_number()
            y1 = read_number()
            x2 = read_number()
            y2 = read_number()
            if cmd == "q":
                x1 += x
                y1 += y
                x2 += x
                y2 += y
            quadratic_bbox(x, y, x1, y1, x2, y2)
            x = x2
            y = y2
            last_quad = (x1, y1)
            last_cubic = None
        elif cmd in "Tt":
            x2 = read_number()
            y2 = read_number()
            if last_quad is not None:
                x1 = 2 * x - last_quad[0]
                y1 = 2 * y - last_quad[1]
            else:
                x1 = x
                y1 = y
            if cmd == "t":
                x2 += x
                y2 += y
            quadratic_bbox(x, y, x1, y1, x2, y2)
            x = x2
            y = y2
            last_quad = (x1, y1)
            last_cubic = None
        elif cmd in "Aa":
            rx = read_number()
            ry = read_number()
            rot = read_number()
            large_arc = int(read_number()) != 0
            sweep = int(read_number()) != 0
            x2 = read_number()
            y2 = read_number()
            if cmd == "a":
                x2 += x
                y2 += y
            arc_bbox(x, y, rx, ry, rot, large_arc, sweep, x2, y2)
            x = x2
            y = y2
            last_quad = None
            last_cubic = None
        else:
            raise ValueError(f"Unsupported SVG path command: {cmd}")

    return bbox


def fmt_number(value):
    if abs(value - round(value)) < 1e-6:
        return str(int(round(value)))
    return f"{value:.4f}".rstrip("0").rstrip(".")


def try_import_smoothing():
    try:
        from shapely import affinity
        from shapely.geometry import LineString, MultiPolygon, Polygon
        from shapely.ops import unary_union
        from svgpathtools import parse_path
    except Exception:
        return None
    return {
        "affinity": affinity,
        "LineString": LineString,
        "MultiPolygon": MultiPolygon,
        "Polygon": Polygon,
        "unary_union": unary_union,
        "parse_path": parse_path,
    }


def path_to_polygons(path_data, sample_points):
    tools = try_import_smoothing()
    if not tools:
        return None

    parse_path = tools["parse_path"]
    LineString = tools["LineString"]
    Polygon = tools["Polygon"]

    path = parse_path(path_data)
    shapes = []
    for subpath in path.continuous_subpaths():
        if subpath.length() == 0:
            continue
        points = []
        for segment in subpath:
            for i in range(sample_points):
                t = i / float(sample_points)
                pt = segment.point(t)
                points.append((pt.real, pt.imag))
        pt = subpath[-1].point(1.0)
        points.append((pt.real, pt.imag))

        if len(points) < 3:
            continue
        if points[0] != points[-1]:
            points.append(points[0])

        try:
            shapes.append(Polygon(points))
        except Exception:
            shapes.append(LineString(points).buffer(0.0))

    if not shapes:
        return None
    return shapes


def path_to_shape(path_data, sample_points):
    tools = try_import_smoothing()
    if not tools:
        return None
    unary_union = tools["unary_union"]
    shapes = path_to_polygons(path_data, sample_points)
    if not shapes:
        return None
    return unary_union(shapes)


def shape_to_path_data(shape):
    tools = try_import_smoothing()
    if not tools:
        return []

    Polygon = tools["Polygon"]
    MultiPolygon = tools["MultiPolygon"]

    def ring_to_path(coords):
        if not coords:
            return ""
        parts = [f"M{fmt_number(coords[0][0])} {fmt_number(coords[0][1])}"]
        for x, y in coords[1:]:
            parts.append(f"L{fmt_number(x)} {fmt_number(y)}")
        parts.append("Z")
        return " ".join(parts)

    paths = []
    if isinstance(shape, Polygon):
        paths.append(ring_to_path(list(shape.exterior.coords)))
        for interior in shape.interiors:
            paths.append(ring_to_path(list(interior.coords)))
    elif isinstance(shape, MultiPolygon):
        for poly in shape.geoms:
            paths.extend(shape_to_path_data(poly))
    return [p for p in paths if p]


def transform_path_data(path_data, scale, translate_x, translate_y):
    tokens = re.findall(
        r"[MmLlHhVvCcSsQqTtAaZz]|[-+]?(?:\d*\.\d+|\d+)(?:[eE][-+]?\d+)?",
        path_data,
    )
    if not tokens:
        return ""

    index = 0
    cmd = None
    x = 0.0
    y = 0.0
    start_x = 0.0
    start_y = 0.0
    last_cubic = None
    last_quad = None
    out = []

    def read_number():
        nonlocal index
        value = float(tokens[index])
        index += 1
        return value

    def transform_point(px, py):
        return px * scale + translate_x, py * scale + translate_y

    while index < len(tokens):
        token = tokens[index]
        if re.match(r"[A-Za-z]", token):
            cmd = token
            index += 1
        elif cmd is None:
            raise ValueError("Path data missing command")

        if cmd in "Mm":
            x1 = read_number()
            y1 = read_number()
            if cmd == "m":
                x1 += x
                y1 += y
            x = x1
            y = y1
            start_x = x
            start_y = y
            tx, ty = transform_point(x, y)
            out.append(f"M{fmt_number(tx)} {fmt_number(ty)}")
            cmd = "L" if cmd == "M" else "l"
            last_cubic = None
            last_quad = None
        elif cmd in "Zz":
            out.append("Z")
            x = start_x
            y = start_y
            last_cubic = None
            last_quad = None
        elif cmd in "Ll":
            x1 = read_number()
            y1 = read_number()
            if cmd == "l":
                x1 += x
                y1 += y
            x = x1
            y = y1
            tx, ty = transform_point(x, y)
            out.append(f"L{fmt_number(tx)} {fmt_number(ty)}")
            last_cubic = None
            last_quad = None
        elif cmd in "Hh":
            x1 = read_number()
            if cmd == "h":
                x1 += x
            x = x1
            tx, ty = transform_point(x, y)
            out.append(f"L{fmt_number(tx)} {fmt_number(ty)}")
            last_cubic = None
            last_quad = None
        elif cmd in "Vv":
            y1 = read_number()
            if cmd == "v":
                y1 += y
            y = y1
            tx, ty = transform_point(x, y)
            out.append(f"L{fmt_number(tx)} {fmt_number(ty)}")
            last_cubic = None
            last_quad = None
        elif cmd in "Cc":
            x1 = read_number()
            y1 = read_number()
            x2 = read_number()
            y2 = read_number()
            x3 = read_number()
            y3 = read_number()
            if cmd == "c":
                x1 += x
                y1 += y
                x2 += x
                y2 += y
                x3 += x
                y3 += y
            x = x3
            y = y3
            last_cubic = (x2, y2)
            last_quad = None
            t1x, t1y = transform_point(x1, y1)
            t2x, t2y = transform_point(x2, y2)
            t3x, t3y = transform_point(x3, y3)
            out.append(
                "C"
                f"{fmt_number(t1x)} {fmt_number(t1y)} "
                f"{fmt_number(t2x)} {fmt_number(t2y)} "
                f"{fmt_number(t3x)} {fmt_number(t3y)}"
            )
        elif cmd in "Ss":
            x2 = read_number()
            y2 = read_number()
            x3 = read_number()
            y3 = read_number()
            if last_cubic is not None:
                x1 = 2 * x - last_cubic[0]
                y1 = 2 * y - last_cubic[1]
            else:
                x1 = x
                y1 = y
            if cmd == "s":
                x2 += x
                y2 += y
                x3 += x
                y3 += y
            x = x3
            y = y3
            last_cubic = (x2, y2)
            last_quad = None
            t1x, t1y = transform_point(x1, y1)
            t2x, t2y = transform_point(x2, y2)
            t3x, t3y = transform_point(x3, y3)
            out.append(
                "C"
                f"{fmt_number(t1x)} {fmt_number(t1y)} "
                f"{fmt_number(t2x)} {fmt_number(t2y)} "
                f"{fmt_number(t3x)} {fmt_number(t3y)}"
            )
        elif cmd in "Qq":
            x1 = read_number()
            y1 = read_number()
            x2 = read_number()
            y2 = read_number()
            if cmd == "q":
                x1 += x
                y1 += y
                x2 += x
                y2 += y
            x = x2
            y = y2
            last_quad = (x1, y1)
            last_cubic = None
            t1x, t1y = transform_point(x1, y1)
            t2x, t2y = transform_point(x2, y2)
            out.append(
                "Q"
                f"{fmt_number(t1x)} {fmt_number(t1y)} "
                f"{fmt_number(t2x)} {fmt_number(t2y)}"
            )
        elif cmd in "Tt":
            x2 = read_number()
            y2 = read_number()
            if last_quad is not None:
                x1 = 2 * x - last_quad[0]
                y1 = 2 * y - last_quad[1]
            else:
                x1 = x
                y1 = y
            if cmd == "t":
                x2 += x
                y2 += y
            x = x2
            y = y2
            last_quad = (x1, y1)
            last_cubic = None
            t1x, t1y = transform_point(x1, y1)
            t2x, t2y = transform_point(x2, y2)
            out.append(
                "Q"
                f"{fmt_number(t1x)} {fmt_number(t1y)} "
                f"{fmt_number(t2x)} {fmt_number(t2y)}"
            )
        elif cmd in "Aa":
            rx = read_number()
            ry = read_number()
            rot = read_number()
            large_arc = int(read_number())
            sweep = int(read_number())
            x2 = read_number()
            y2 = read_number()
            if cmd == "a":
                x2 += x
                y2 += y
            x = x2
            y = y2
            last_quad = None
            last_cubic = None
            tx, ty = transform_point(x2, y2)
            out.append(
                "A"
                f"{fmt_number(rx * scale)} {fmt_number(ry * scale)} "
                f"{fmt_number(rot)} {large_arc} {sweep} "
                f"{fmt_number(tx)} {fmt_number(ty)}"
            )
        else:
            raise ValueError(f"Unsupported SVG path command: {cmd}")

    return " ".join(out)


def build_vector_xml(base, overlay, scale, gap_dp, smooth, sample_points, simplify_dp):
    width_dp = base["width"] or base["vb_width"]
    height_dp = base["height"] or base["vb_height"]

    if width_dp:
        gap = gap_dp * (base["vb_width"] / width_dp)
        simplify = simplify_dp * (base["vb_width"] / width_dp)
    else:
        gap = gap_dp
        simplify = simplify_dp

    base_offset_x = -base["min_x"]
    base_offset_y = -base["min_y"]

    overlay_offset_x = -overlay["min_x"]
    overlay_offset_y = -overlay["min_y"]

    overlay_bbox = None
    for path in overlay["paths"]:
        overlay_bbox = bbox_union(overlay_bbox, path.get("bbox"))

    if overlay_bbox is None:
        overlay_bbox = (
            overlay["min_x"],
            overlay["min_y"],
            overlay["min_x"] + overlay["vb_width"],
            overlay["min_y"] + overlay["vb_height"],
        )

    overlay_bbox_norm = bbox_translate(overlay_bbox, overlay_offset_x, overlay_offset_y)
    base_bbox = None
    for path in base["paths"]:
        base_bbox = bbox_union(base_bbox, path.get("bbox"))

    if base_bbox is None:
        base_bbox = (0.0, 0.0, base["vb_width"], base["vb_height"])

    base_bbox_shifted = bbox_translate(base_bbox, base_offset_x, base_offset_y)
    base_center_x = (base_bbox_shifted[0] + base_bbox_shifted[2]) / 2.0
    base_center_y = (base_bbox_shifted[1] + base_bbox_shifted[3]) / 2.0
    viewport_center_x = base["vb_width"] / 2.0
    viewport_center_y = base["vb_height"] / 2.0
    center_dx = viewport_center_x - base_center_x
    center_dy = viewport_center_y - base_center_y

    base_translate_x = base_offset_x + center_dx
    base_translate_y = base_offset_y + center_dy

    overlay_center_x = (overlay_bbox_norm[0] + overlay_bbox_norm[2]) / 2.0
    overlay_center_y = (overlay_bbox_norm[1] + overlay_bbox_norm[3]) / 2.0

    overlay_scaled_bbox = bbox_scale(overlay_bbox_norm, scale)
    overlay_target_tx = base["vb_width"] - overlay_scaled_bbox[2]
    overlay_target_ty = -overlay_scaled_bbox[1]

    overlay_translate_x = overlay_target_tx
    overlay_translate_y = overlay_target_ty

    max_dim = max(
        overlay_bbox_norm[2] - overlay_bbox_norm[0],
        overlay_bbox_norm[3] - overlay_bbox_norm[1],
    )
    if max_dim <= 0:
        hole_scale = scale
    else:
        hole_scale = scale * (1.0 + (2.0 * gap) / max_dim)

    overlay_center_target_x = overlay_center_x * scale + overlay_target_tx
    overlay_center_target_y = overlay_center_y * scale + overlay_target_ty

    hole_target_tx = overlay_center_target_x - overlay_center_x * hole_scale
    hole_target_ty = overlay_center_target_y - overlay_center_y * hole_scale

    hole_translate_x = hole_target_tx
    hole_translate_y = hole_target_ty

    overlay_paths = []
    for path in overlay["paths"]:
        overlay_paths.append(
            (
                transform_path_data(
                    path["d"],
                    scale,
                    overlay_offset_x * scale + overlay_translate_x,
                    overlay_offset_y * scale + overlay_translate_y,
                ),
                path["fill"],
            )
        )

    hole_paths = []
    smooth_used = False
    smooth_base_paths = None
    if gap > 0:
        if smooth:
            tools = try_import_smoothing()
            if tools:
                affinity = tools["affinity"]
                hole_shapes = []
                for path in overlay["paths"]:
                    shape = path_to_shape(path["d"], sample_points)
                    if shape is None:
                        continue
                    shape = affinity.translate(
                        shape,
                        xoff=overlay_offset_x,
                        yoff=overlay_offset_y,
                    )
                    shape = affinity.scale(
                        shape, xfact=scale, yfact=scale, origin=(0, 0)
                    )
                    shape = affinity.translate(
                        shape,
                        xoff=overlay_translate_x,
                        yoff=overlay_translate_y,
                    )
                    hole_shapes.append(shape)

                if hole_shapes:
                    union = tools["unary_union"](hole_shapes)
                    hole_shape = union.buffer(
                        gap,
                        join_style=1,
                        cap_style=1,
                        resolution=16,
                    )
                    if simplify > 0:
                        hole_shape = hole_shape.simplify(
                            simplify, preserve_topology=True
                        )
                    hole_paths = shape_to_path_data(hole_shape)

                    base_shapes = []
                    for path in base["paths"]:
                        polys = path_to_polygons(path["d"], sample_points)
                        if not polys:
                            continue
                        for poly in polys:
                            poly = affinity.translate(
                                poly,
                                xoff=base_translate_x,
                                yoff=base_translate_y,
                            )
                            base_shapes.append(poly)

                    if base_shapes:
                        base_shape = base_shapes[0]
                        for poly in base_shapes[1:]:
                            base_shape = base_shape.symmetric_difference(poly)
                        base_cut = base_shape.difference(hole_shape)
                        if simplify > 0:
                            base_cut = base_cut.simplify(
                                simplify, preserve_topology=True
                            )
                        smooth_base_paths = shape_to_path_data(base_cut)
                        smooth_used = True
            else:
                logging.warning(
                    "Smooth gap requested but shapely/svgpathtools not available; falling back to sharp gap."
                )
        if not hole_paths:
            for path in overlay["paths"]:
                hole_paths.append(
                    transform_path_data(
                        path["d"],
                        hole_scale,
                        overlay_offset_x * hole_scale + hole_translate_x,
                        overlay_offset_y * hole_scale + hole_translate_y,
                    )
                )

    lines = []
    lines.append('<vector xmlns:android="http://schemas.android.com/apk/res/android"')
    lines.append(f'    android:width="{fmt_number(width_dp)}dp"')
    lines.append(f'    android:height="{fmt_number(height_dp)}dp"')
    lines.append(f'    android:viewportWidth="{fmt_number(base["vb_width"])}"')
    lines.append(f'    android:viewportHeight="{fmt_number(base["vb_height"])}">')

    logging.debug(
        "Base translate=(%s, %s), overlay translate=(%s, %s), gap=%s (vb units)",
        fmt_number(base_translate_x),
        fmt_number(base_translate_y),
        fmt_number(overlay_translate_x),
        fmt_number(overlay_translate_y),
        fmt_number(gap),
    )
    logging.debug(
        "Overlay scale=%s, hole scale=%s",
        fmt_number(scale),
        fmt_number(hole_scale),
    )
    logging.debug(
        "Overlay bbox norm=(%s, %s, %s, %s)",
        fmt_number(overlay_bbox_norm[0]),
        fmt_number(overlay_bbox_norm[1]),
        fmt_number(overlay_bbox_norm[2]),
        fmt_number(overlay_bbox_norm[3]),
    )
    logging.debug(
        "Gap=%s dp -> %s viewBox units",
        fmt_number(gap_dp),
        fmt_number(gap),
    )
    logging.debug(
        "Smooth gap=%s (sample points=%s, simplify=%s)",
        "on" if smooth_used else "off",
        sample_points,
        fmt_number(simplify),
    )
    logging.debug(
        "Overlay target translate=(%s, %s)",
        fmt_number(overlay_target_tx),
        fmt_number(overlay_target_ty),
    )

    path_indent = "    "

    if smooth_base_paths:
        combined_base = " ".join(smooth_base_paths)
        fill_type = ""
    else:
        transformed_base_paths = []
        for path in base["paths"]:
            transformed_base_paths.append(
                transform_path_data(path["d"], 1.0, base_translate_x, base_translate_y)
            )

        combined_base = " ".join(transformed_base_paths)
        if hole_paths:
            combined_base = f"{combined_base} {' '.join(hole_paths)}"
        fill_type = ' android:fillType="evenOdd"' if hole_paths else ""
    lines.append(
        f'{path_indent}<path android:fillColor="@android:color/white"{fill_type} '
        f'android:pathData="{combined_base}" />'
    )

    for path_data, _fill in overlay_paths:
        lines.append(
            f'{path_indent}<path android:fillColor="@android:color/white" '
            f'android:pathData="{path_data}" />'
        )

    lines.append("</vector>")

    return "\n".join(lines) + "\n"


def download_material_icon(icon_name, cache_dir=None):
    """Download a Material Symbol icon by name."""
    if cache_dir is None:
        cache_dir = tempfile.gettempdir()

    cache_path = os.path.join(cache_dir, f"{icon_name}_material_icon.svg")

    # Return cached file if it exists
    if os.path.exists(cache_path):
        logging.debug("Using cached icon: %s", cache_path)
        return cache_path

    url = f"https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsoutlined/{icon_name}/default/24px.svg"
    logging.info("Downloading %s from Material Icons...", icon_name)
    logging.debug("URL: %s", url)

    headers = {
        "User-Agent": "Mozilla/5.0 (X11; Linux x86_64; rv:147.0) Gecko/20100101 Firefox/147.0",
        "Accept": "*/*",
        "Accept-Language": "en-US,en;q=0.9",
        "Accept-Encoding": "gzip, deflate, br, zstd",
        "Referer": "https://fonts.google.com/",
        "Origin": "https://fonts.google.com",
        "Sec-GPC": "1",
        "Connection": "keep-alive",
        "Sec-Fetch-Dest": "empty",
        "Sec-Fetch-Mode": "cors",
        "Sec-Fetch-Site": "cross-site",
        "Priority": "u=4",
        "TE": "trailers",
    }

    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req) as response:
            if response.status != 200:
                raise ValueError(
                    f"Failed to download {icon_name}: HTTP {response.status}"
                )

            svg_data = response.read()

            # Decompress if gzip compressed
            if svg_data[:2] == b"\x1f\x8b":  # gzip magic number
                svg_data = gzip.decompress(svg_data)

            with open(cache_path, "wb") as f:
                f.write(svg_data)

            logging.info("Downloaded %s successfully", icon_name)
            return cache_path
    except Exception as exc:
        raise ValueError(f"Failed to download icon '{icon_name}': {exc}")


def resolve_input_path(input_arg, cache_dir=None):
    """Resolve input argument to a file path, downloading if necessary."""
    # Check if it's an existing file
    if os.path.isfile(input_arg):
        return input_arg

    # Check if it's an absolute path that doesn't exist
    if os.path.isabs(input_arg):
        if not os.path.exists(input_arg):
            raise ValueError(f"File not found: {input_arg}")
        return input_arg

    # Check if it looks like a relative path to an existing file
    if os.path.sep in input_arg or input_arg.endswith(".svg"):
        if os.path.exists(input_arg):
            return input_arg
        raise ValueError(f"File not found: {input_arg}")

    # Treat as icon name and download
    return download_material_icon(input_arg, cache_dir)


def main():
    parser = argparse.ArgumentParser(
        description=(
            "Compose two SVGs into a single Android vector drawable with a smaller "
            "overlay in the top-right corner. Supports both file paths and Material Icon names."
        )
    )
    parser.add_argument(
        "base_svg", help="Base/background SVG file path or Material Icon name"
    )
    parser.add_argument(
        "overlay_svg", help="Overlay SVG file path or Material Icon name"
    )
    parser.add_argument(
        "output_xml", help="Output Android drawable XML file path or name"
    )
    parser.add_argument(
        "--scale",
        type=float,
        default=0.5,
        help="Scale for the overlay icon (default: 0.5)",
    )
    parser.add_argument(
        "--gap",
        type=float,
        default=2.0,
        help="Gap between base and overlay in dp units (default: 2)",
    )
    parser.add_argument(
        "--smooth",
        action="store_true",
        help="Use shapely/svgpathtools to create a rounded gap",
    )
    parser.add_argument(
        "--sample-points",
        type=int,
        default=30,
        help="Samples per curve segment for smoothing (default: 30)",
    )
    parser.add_argument(
        "--smooth-simplify",
        type=float,
        default=0.25,
        help="Simplify tolerance in dp units for smooth mode (default: 0.25)",
    )
    parser.add_argument(
        "--verbose",
        action="store_true",
        help="Enable debug logging",
    )
    parser.add_argument(
        "--trim-distance",
        type=float,
        default=None,
        help="Deprecated alias for --gap",
    )
    parser.add_argument(
        "--cache-dir",
        type=str,
        default=None,
        help="Directory to cache downloaded icons (default: system temp directory)",
    )
    parser.add_argument(
        "--output-dir",
        type=str,
        default=None,
        help="Directory for output file when output_xml is just a name (default: current directory)",
    )

    args = parser.parse_args()
    if args.scale <= 0:
        parser.error("--scale must be > 0")
    if args.sample_points < 6:
        parser.error("--sample-points must be >= 6")
    if args.smooth_simplify < 0:
        parser.error("--smooth-simplify must be >= 0")

    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="[%(levelname)s] %(message)s",
    )

    try:
        # Resolve input paths (download if necessary)
        base_svg_path = resolve_input_path(args.base_svg, args.cache_dir)
        overlay_svg_path = resolve_input_path(args.overlay_svg, args.cache_dir)

        # Resolve output path
        output_xml_path = args.output_xml
        if not os.path.isabs(output_xml_path) and os.path.sep not in output_xml_path:
            # Just a name, not a path
            if not output_xml_path.endswith(".xml"):
                output_xml_path = f"{output_xml_path}.xml"

            if args.output_dir:
                output_xml_path = os.path.join(args.output_dir, output_xml_path)

        logging.info("Base SVG: %s", base_svg_path)
        logging.info("Overlay SVG: %s", overlay_svg_path)
        logging.info("Output XML: %s", output_xml_path)
        logging.info("Scale: %s", fmt_number(args.scale))
        gap = args.gap if args.trim_distance is None else args.trim_distance
        logging.info("Gap: %s", fmt_number(gap))
        logging.info("Smooth: %s", "on" if args.smooth else "off")
        logging.info("Smooth simplify: %s", fmt_number(args.smooth_simplify))

        base = parse_svg(base_svg_path)
        overlay = parse_svg(overlay_svg_path)
        xml = build_vector_xml(
            base,
            overlay,
            args.scale,
            gap,
            args.smooth,
            args.sample_points,
            args.smooth_simplify,
        )
    except Exception as exc:
        print(f"Error: {exc}", file=sys.stderr)
        if args.verbose:
            import traceback

            traceback.print_exc()
        return 1

    # Ensure output directory exists
    output_dir = os.path.dirname(output_xml_path)
    if output_dir and not os.path.exists(output_dir):
        os.makedirs(output_dir, exist_ok=True)

    with open(output_xml_path, "w", encoding="utf-8") as f:
        f.write(xml)

    logging.info("Output written to: %s", output_xml_path)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
