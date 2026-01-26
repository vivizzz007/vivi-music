package com.music.vivi.bluetooth

// fun getBluetoothBatteryLevel(context: Context, device: BluetoothDevice): Int? {
//    return try {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            if (checkBluetoothPermission(context)) {
//                // Use reflection to access batteryLevel property to avoid direct reference
//                val method = device.javaClass.getMethod("getBatteryLevel")
//                val batteryLevel = method.invoke(device) as? Int
//                batteryLevel?.takeIf { it >= 0 } // Returns -1 if unavailable
//            } else {
//                null
//            }
//        } else {
//            // Fallback for older APIs using reflection
//            try {
//                val method = device.javaClass.getMethod("getBatteryLevel")
//                val batteryLevel = method.invoke(device) as? Int
//                batteryLevel?.takeIf { it >= 0 }
//            } catch (e: NoSuchMethodException) {
//                null // Method not available
//            } catch (e: Exception) {
//                null // Other errors
//            }
//        }
//    } catch (e: SecurityException) {
//        null // Permission denied
//    } catch (e: Exception) {
//        null // General error handling
//    }
// }
