import xml.etree.ElementTree as ET

metro_path = r'C:\Vividh\vivimusic-13.1.1\app\src\main\res\values\strings.xml'
alpha_path = r'C:\Vividh\vivimusic-13.1.1\vivi-music-alpha\app\src\main\res\values\strings.xml'

ET.register_namespace('', 'http://schemas.android.com/tools')

tree_metro = ET.parse(metro_path)
root_metro = tree_metro.getroot()

tree_alpha = ET.parse(alpha_path)
root_alpha = tree_alpha.getroot()

metro_keys = set()
for child in root_metro:
    if 'name' in child.attrib:
        metro_keys.add(child.attrib['name'])

added_count = 0
for child in root_alpha:
    if 'name' in child.attrib and child.attrib['name'] not in metro_keys:
        root_metro.append(child)
        added_count += 1

tree_metro.write(metro_path, encoding='utf-8', xml_declaration=True)
print(f'Successfully added {added_count} missing strings.')
