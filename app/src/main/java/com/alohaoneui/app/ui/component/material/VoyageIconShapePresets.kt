package com.alohaoneui.app.ui.component.material

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.core.graphics.PathParser
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.TransformResult
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Voyage's source-ordered 51 icon-shape presets.
 *
 * The first 26 entries are the original built-in Compose/path shapes from vqp.
 * The final 25 entries are the RoundedPolygon definitions from qha0, expanded
 * with the same radial/mirror symmetry rules and per-vertex CornerRounding.
 */
internal object VoyageIconShapePresets {
    const val COUNT = 51

    private const val GOP_2_PATH = """M 12.000000 100.000000 C 12.000000 76.000000 0.000000 77.614200 0.000000 50.000000 C 0.000000 22.385800 22.385800 0.000000 50.000000 0.000000 C 77.614200 0.000000 76.000000 12.000000 100.000000 12.000000 C 124.000000 12.000000 122.385800 0.000000 150.000000 0.000000 C 177.614200 0.000000 200.000000 22.385800 200.000000 50.000000 C 200.000000 77.614200 188.000000 76.000000 188.000000 100.000000 C 188.000000 124.000000 200.000000 122.385800 200.000000 150.000000 C 200.000000 177.614200 177.614200 200.000000 150.000000 200.000000 C 122.385800 200.000000 124.000000 188.000000 100.000000 188.000000 C 76.000000 188.000000 77.614200 200.000000 50.000000 200.000000 C 22.385800 200.000000 0.000000 177.614200 0.000000 150.000000 C 0.000000 122.385800 12.000000 124.000000 12.000000 100.000000 Z"""
    private const val GOP_4_PATH = """M 403.391300 8.735600 C 421.078700 -2.911900 444.002000 -2.911900 461.689400 8.735600 L 518.743000 46.306600 C 528.283900 52.589500 539.599500 55.621500 551.003600 54.950800 L 619.198900 50.940200 C 640.340400 49.696800 660.192600 61.158500 669.686500 80.089200 L 700.310900 141.153400 C 705.432100 151.365000 713.715700 159.648600 723.927300 164.769900 L 784.991500 195.394200 C 803.922200 204.888100 815.383900 224.740300 814.140600 245.881800 L 810.129900 314.077100 C 809.459300 325.481200 812.491300 336.796900 818.774200 346.337800 L 856.345100 403.391300 C 867.992600 421.078700 867.992700 444.002000 856.345200 461.689400 L 818.774200 518.743000 C 812.491300 528.283900 809.459300 539.599500 810.129900 551.003600 L 814.140600 619.198900 C 815.383900 640.340400 803.922300 660.192600 784.991600 669.686500 L 723.927400 700.310900 C 713.715800 705.432100 705.432100 713.715700 700.310900 723.927300 L 669.686600 784.991500 C 660.192600 803.922200 640.340400 815.383900 619.198900 814.140600 L 551.003600 810.129900 C 539.599500 809.459300 528.283900 812.491300 518.743000 818.774200 L 461.689400 856.345100 C 444.002100 867.992600 421.078700 867.992700 403.391400 856.345200 L 346.337800 818.774200 C 336.796900 812.491300 325.481200 809.459300 314.077100 810.129900 L 245.881800 814.140600 C 224.740400 815.383900 204.888200 803.922300 195.394200 784.991600 L 164.769900 723.927400 C 159.648600 713.715800 151.365000 705.432100 141.153400 700.310900 L 80.089200 669.686600 C 61.158500 660.192600 49.696800 640.340400 50.940200 619.199000 L 54.950800 551.003600 C 55.621500 539.599500 52.589500 528.283900 46.306600 518.743000 L 8.735600 461.689400 C -2.911900 444.002100 -2.911900 421.078700 8.735600 403.391400 L 46.306600 346.337800 C 52.589500 336.796900 55.621500 325.481300 54.950800 314.077100 L 50.940200 245.881800 C 49.696800 224.740400 61.158500 204.888200 80.089200 195.394200 L 141.153400 164.769900 C 151.365000 159.648600 159.648600 151.365000 164.769900 141.153400 L 195.394200 80.089200 C 204.888200 61.158500 224.740300 49.696800 245.881800 50.940200 L 314.077100 54.950800 C 325.481300 55.621500 336.796900 52.589500 346.337800 46.306600 L 403.391300 8.735600 Z"""
    private const val GOP_5_PATH = """M 500.000000 0.000000 L 853.553400 146.446600 L 1000.000000 500.000000 L 853.553400 853.553400 L 500.000000 1000.000000 L 146.446600 853.553400 L 0.000000 500.000000 L 146.446600 146.446600 L 500.000000 0.000000 Z"""
    private const val GOP_6_PATH = """M 1000.000000 500.000000 C 1000.000000 776.142400 776.142400 1000.000000 500.000000 1000.000000 C 223.857600 1000.000000 0.000000 776.142400 0.000000 500.000000 C 0.000000 223.857600 223.857600 0.000000 500.000000 0.000000 C 776.142400 0.000000 1000.000000 223.857600 1000.000000 500.000000 Z"""
    private const val GOP_7_PATH = """M 521.133000 28.588000 C 575.782900 -9.529300 648.402800 -9.529300 703.052800 28.588000 L 1156.133200 344.602900 C 1214.148000 385.067100 1238.457200 458.990200 1215.780800 525.989200 L 1045.410000 1029.362500 C 1023.555500 1093.933300 962.971600 1137.388200 894.802600 1137.388200 L 329.383200 1137.388200 C 261.214200 1137.388200 200.630200 1093.933300 178.775700 1029.362500 L 8.405000 525.989300 C -14.271400 458.990300 10.037700 385.067100 68.052600 344.602900 L 521.133000 28.588000 Z"""
    private const val GOP_9_PATH = """M 12.000000 21.350000 L 10.550000 20.030000 C 5.400000 15.360000 2.000000 12.280000 2.000000 8.500000 C 2.000000 5.420000 4.420000 3.000000 7.500000 3.000000 C 9.240000 3.000000 10.910000 3.810000 12.000000 5.090000 C 13.090000 3.810000 14.760000 3.000000 16.500000 3.000000 C 19.580000 3.000000 22.000000 5.420000 22.000000 8.500000 C 22.000000 12.280000 18.600000 15.360000 13.450000 20.040000 L 12.000000 21.350000 Z"""
    private const val GOP_10_PATH = """M 2537.800000 975.157000 C 2623.020000 1101.610000 2665.640000 1164.840000 2683.130000 1233.150000 C 2699.690000 1297.820000 2700.130000 1365.570000 2684.420000 1430.450000 C 2667.810000 1498.980000 2626.030000 1562.760000 2542.460000 1690.320000 C 2517.240000 1728.810000 2504.640000 1748.050000 2494.330000 1768.430000 C 2484.530000 1787.800000 2476.300000 1807.930000 2469.700000 1828.610000 C 2462.770000 1850.370000 2458.260000 1872.960000 2449.250000 1918.150000 C 2419.300000 2068.370000 2404.330000 2143.480000 2367.900000 2204.190000 C 2334.180000 2260.380000 2287.430000 2307.620000 2231.590000 2341.920000 C 2171.260000 2378.980000 2095.520000 2394.900000 1944.040000 2426.740000 C 1897.480000 2436.530000 1874.190000 2441.420000 1852.040000 2448.800000 C 1832.550000 2455.290000 1813.570000 2463.250000 1795.280000 2472.600000 C 1774.490000 2483.220000 1754.880000 2496.270000 1715.650000 2522.350000 C 1584.840000 2609.340000 1519.440000 2652.830000 1448.720000 2669.700000 C 1388.230000 2684.120000 1325.250000 2684.530000 1264.590000 2670.890000 C 1193.650000 2654.950000 1127.690000 2612.310000 995.759000 2527.040000 C 956.196000 2501.470000 936.414000 2488.680000 915.487000 2478.330000 C 897.073000 2469.210000 877.998000 2461.510000 858.423000 2455.270000 C 836.176000 2448.180000 812.831000 2443.590000 766.142000 2434.410000 C 614.258000 2404.540000 538.317000 2389.610000 477.510000 2353.340000 C 421.231000 2319.770000 373.865000 2273.140000 339.421000 2217.400000 C 302.205000 2157.160000 286.253000 2082.260000 254.349000 1932.440000 C 244.752000 1887.380000 239.953000 1864.840000 232.735000 1843.180000 C 225.871000 1822.580000 217.375000 1802.560000 207.326000 1783.320000 C 196.758000 1763.080000 183.900000 1744.000000 158.185000 1705.840000 C 72.963700 1579.380000 30.353100 1516.160000 12.857700 1447.850000 C -3.705600 1383.180000 -4.146700 1315.430000 11.573100 1250.550000 C 28.177500 1182.010000 69.961200 1118.240000 153.529000 990.681000 C 178.745000 952.192000 191.353000 932.947000 201.657000 912.571000 C 211.454000 893.196000 219.689000 873.069000 226.284000 852.384000 C 233.219000 830.629000 237.724000 808.034000 246.734000 762.845000 C 276.684000 612.629000 291.659000 537.521000 328.088000 476.810000 C 361.804000 420.620000 408.558000 373.377000 464.395000 339.080000 C 524.724000 302.023000 600.465000 286.102000 751.947000 254.260000 C 798.513000 244.472000 821.796000 239.578000 843.949000 232.199000 C 863.440000 225.707000 882.414000 217.752000 900.708000 208.401000 C 921.498000 197.775000 941.112000 184.732000 980.339000 158.647000 C 1111.140000 71.662600 1176.550000 28.170600 1247.270000 11.302600 C 1307.750000 -3.122000 1370.730000 -3.532100 1431.400000 10.103800 C 1502.340000 26.049300 1568.300000 68.686000 1700.230000 153.959000 C 1739.790000 179.532000 1759.570000 192.318000 1780.500000 202.673000 C 1798.910000 211.784000 1817.990000 219.492000 1837.570000 225.730000 C 1859.810000 232.819000 1883.160000 237.410000 1929.850000 246.591000 C 2081.730000 276.457000 2157.670000 291.391000 2218.480000 327.659000 C 2274.760000 361.227000 2322.120000 407.856000 2356.570000 463.603000 C 2393.780000 523.834000 2409.740000 598.741000 2441.640000 748.554000 C 2451.240000 793.622000 2456.040000 816.156000 2463.250000 837.819000 C 2470.120000 858.417000 2478.610000 878.434000 2488.660000 897.680000 C 2499.230000 917.921000 2512.090000 937.000000 2537.800000 975.157000 Z"""
    private const val GOP_11_PATH = """M 0.000000 500.000000 C 0.000000 88.250000 88.250000 0.000000 500.000000 0.000000 C 911.750000 0.000000 1000.000000 88.250000 1000.000000 500.000000 C 1000.000000 911.750000 911.750000 1000.000000 500.000000 1000.000000 C 88.250000 1000.000000 0.000000 911.750000 0.000000 500.000000 Z"""
    private const val VECTOR_MO9_PATH = """M10 0C4.47715 0 0 4.47715 0 10V44.3522C0 46.1924 0.832489 48.0327 4 47.9996C5.04582 47.9996 11.1957 46.5617 16.3492 45.3564C18.5872 44.833 20.6375 44.3535 22 44.0549C22.5203 43.9314 23 44.0549 23 44.0549C26.0208 44.6465 29.2917 45.4016 32.2846 46.0925C36.4967 47.0648 40.158 47.91 41.796 47.9935L42.0617 47.9925C45.2368 47.8668 46 46.0449 46 43.8424V10C46 4.47715 41.5228 0 36 0H10Z"""
    private const val VECTOR_HR70_PATH = """M48 20.4342C48 9.14772 38.8281 0 27.5193 0C21.8632 0 16.743 2.28863 13.036 5.98382L5.99744 13.1154C2.29384 16.814 0 21.9225 0 27.5658C0 38.8523 9.16854 48 20.4807 48C26.1368 48 31.257 45.7114 34.964 42.0162L42.0026 34.8846C45.7096 31.186 48 26.0775 48 20.4342Z"""
    private const val VECTOR_F1A_PATH = """M0.369565 7.44186C0.369565 3.33212 3.67833 0 7.76087 0H43.6087C47.6912 0 51 3.33212 51 7.44186V8.55814C51 12.6679 47.6912 16 43.6087 16C47.6912 16 51 19.3321 51 23.4419V24.5581C51 28.6679 47.6912 32 43.6087 32H43.2391C47.3217 32 50.6304 35.3321 50.6304 39.4419V40.5581C50.6304 44.6679 47.3217 48 43.2391 48H7.3913C3.30876 48 0 44.6679 0 40.5581V39.4419C0 35.3321 3.30876 32 7.3913 32H7.76087C3.67833 32 0.369565 28.6679 0.369565 24.5581V23.4419C0.369565 19.3321 3.67833 16 7.76087 16C3.67833 16 0.369565 12.6679 0.369565 8.55814V7.44186Z"""
    private const val VECTOR_BME0_PATH = """M22.8468 0.551384C24.8376 2.58461 34.1326 7.39938 41.7392 9.94215C42.1061 10.0645 42.4253 10.2994 42.6515 10.6135C42.8778 10.9276 42.9997 11.305 43 11.6923C43 32.5957 39.9697 42.7864 22.0137 47.9286C21.6828 48.0238 21.3319 48.0238 21.0011 47.9286C3.14341 42.8184 1.89533e-09 32.2757 1.89533e-09 12.0123C-1.82005e-05 11.6104 0.131069 11.2195 0.373309 10.8992C0.615549 10.5788 0.955673 10.3465 1.34191 10.2375C8.26315 8.39719 14.7081 5.08702 20.2392 0.531692C20.5968 0.202432 21.0611 0.0136179 21.5467 0C21.789 0.00155783 22.0286 0.051074 22.2517 0.145701C22.4748 0.240328 22.6771 0.378198 22.8468 0.551384Z"""
    private const val VECTOR_LHJ_PATH = """M37 29.0542C36.8496 35.2815 34.6566 40.4326 29.781 44.0763C24.0677 48.3452 17.7493 49.1448 11.1127 46.4063C6.52734 44.5131 3.32181 41.1353 1.35268 36.5987C0.595451 34.8535 0.132022 32.9832 0.0428338 31.0619C-0.236972 25.0372 0.852523 19.2731 3.35679 13.7856C5.28745 9.55367 7.91064 5.81135 11.3033 2.65723C13.3004 0.799282 15.7172 0.0455345 18.4891 0.00150717C22.8523 -0.0689365 25.743 2.33848 28.263 5.43624C30.0888 7.67987 31.7082 10.0732 32.997 12.6972C35.0484 16.871 36.346 21.2403 36.7639 25.8755C36.8636 26.9849 36.93 28.0962 37 29.056V29.0542Z"""
    private const val VECTOR_A8J_PATH = """M24 0C10.7452 0 0 10.7452 0 24C0 24.2317 0.00328295 24.4626 0.00980488 24.6927C0.00329738 24.8032 0 24.9144 0 25.0264V42.3896C0 45.4881 2.51186 48 5.61039 48H23.2847C23.3623 48 23.4396 47.9984 23.5167 47.9952C23.6774 47.9984 23.8385 48 24 48C37.2548 48 48 37.2548 48 24C48 10.7452 37.2548 0 24 0Z"""
    private const val VECTOR_D06_PATH = """M0.00127574 36.4191C0.00127574 42.8145 5.14768 48 11.4949 48C13.2236 48 14.863 47.6147 16.3341 46.9262C16.3341 46.9262 16.343 46.9223 16.3583 46.9146C16.496 46.8491 16.6337 46.7823 16.7676 46.7117C18.1252 46.0476 22.3143 44.0656 24.2634 43.9205C26.6065 43.7458 30.7293 45.0136 30.7293 45.0136L34.1891 46.6885C34.4147 46.808 34.6455 46.9185 34.88 47.0225L34.8915 47.0289H34.8953C35.8655 47.4579 36.9057 47.7559 37.9944 47.9011H37.997L38.0033 47.9024C38.2914 47.9409 38.5834 47.9679 38.8778 47.9833H38.8855C39.0907 47.9949 39.2973 48 39.5063 48C45.8536 48 51 42.8145 51 36.4191V36.3703C51 34.1995 50.4059 32.1687 49.3746 30.4321L35.905 6.40702C34.0195 2.6088 30.1225 1.30422e-06 25.6198 1.30422e-06C21.1172 1.30422e-06 17.2214 2.6088 15.3347 6.40702L1.88672 30.0133C1.63175 30.4051 1.39846 30.8123 1.19067 31.2349L1.18557 31.2439C0.427059 32.7904 -3.11934e-07 34.5296 -3.11934e-07 36.3703V36.4191H0.00127574Z"""
    private const val VECTOR_IPE0_PATH = """M46.1183 46.1349C46.1183 46.1349 44.5771 44.149 46.1183 41.3152 55.7085 25.337 50.6851 6.5989 50.6851 6.5989L46.1183 9.8651C46.1183 9.8651 44.0233 11.2763 41.4317 9.8651 25.4025.2998 6.599 5.3129 6.599 5.3129L9.8756 9.8651C9.8756 9.8651 11.5539 11.5722 9.8756 14.6905 9.8185 14.7929 9.7729 14.8783 9.7215 14.9579.3483 30.8736 5.3146 49.4011 5.3146 49.4011L9.8813 46.1349C9.8813 46.1349 11.8621 44.6668 14.5622 46.1349 30.5971 55.7002 49.395 50.6871 49.395 50.6871L46.1183 46.1349Z"""
    private const val VECTOR_NSK_PATH = """M24.3629 0.130582C24.0071 -0.0526879 23.5886 -0.0422153 23.2434 0.156763C21.2763 1.29304 14.6586 5.04745 13.1833 5.04745C11.9748 5.04745 6.27256 2.52356 2.91398 0.984095C1.97755 0.55472 0.988809 1.45012 1.31839 2.42407C2.57394 6.1523 4.68221 12.6505 4.30554 13.2684C3.84518 14.0224 0.873717 21.6936 0.0890009 23.7357C-0.0313222 24.0499 -0.0103964 24.3955 0.146547 24.6992C1.13529 26.579 4.72929 33.5695 4.44156 35.1299C4.20614 36.4023 2.04556 42.0941 0.790014 45.3406C0.434276 46.2622 1.3027 47.1785 2.23912 46.8801C5.82789 45.7386 12.3463 43.686 12.9217 43.686C13.6227 43.686 21.925 47.0791 23.9601 47.9116C24.2478 48.0321 24.5669 48.0268 24.8546 47.9116C26.9211 47.0476 35.4745 43.487 35.8302 43.487C36.1179 43.487 41.7417 45.6286 44.9381 46.8591C45.8379 47.2047 46.743 46.3774 46.4866 45.4506C45.5554 42.0994 43.9285 36.1143 43.9756 35.3969C44.0331 34.5225 47.1458 26.223 47.9358 24.1284C48.0508 23.8247 48.0352 23.4844 47.8887 23.1912C46.9784 21.3689 43.6512 14.5984 43.7088 12.8704C43.7558 11.5038 45.6653 5.93238 46.8738 2.53403C47.2243 1.54961 46.2094 0.633265 45.2625 1.08358C41.7731 2.75396 35.9348 5.51871 35.4221 5.51871C34.7891 5.51871 26.6072 1.29827 24.3629 0.130582Z"""
    private const val VECTOR_PEX_PATH = """M25.0702 4.38833C28.2859 2.26692 31.4412 0.185367 35.9294 0.00690002C41.3166 -0.204493 48 4.51158 48 4.51158L47.914 43.5389C41.8695 39.5777 37.3361 38.7019 34.1848 38.9888C29.4365 39.4182 26.0316 41.6001 22.7395 43.7097C19.4728 45.803 16.3173 47.8251 12.0706 47.9931C6.68339 48.2045 0 43.4884 0 43.4884V4.80351C5.84797 8.59348 10.4858 9.24276 13.8152 9.01123C18.563 8.68117 21.847 6.51473 25.0702 4.38833Z"""

    val all: List<Shape> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        listOf(
            gopPath(GOP_11_PATH, 1000f, 1000f),                         // 0: o7g.OI
            RoundedCornerShape(15),                                    // 1: tcc0.l(15)
            RoundedCornerShape(25),                                    // 2: tcc0.l(25)
            RoundedCornerShape(35),                                    // 3: tcc0.l(35)
            RoundedCornerShape(45),                                    // 4: tcc0.l(45)
            CutCornerShape(25),                                        // 5: umb.O(25)
            CutCornerShape(35),                                        // 6: umb.O(35)
            CutCornerShape(50),                                        // 7: umb.O(50)
            gopPath(GOP_2_PATH, 200f, 200f),                            // 8: c9c0.lo
            gopPath(GOP_4_PATH, 865.0807f, 865.0807f),                  // 9: o7g.Il
            gopPath(GOP_10_PATH, 2695.79f, 2680.65f),                   // 10: wdp0.lo
            vectorPath(VECTOR_MO9_PATH),                               // 11: mo9.l
            vectorPath(VECTOR_HR70_PATH),                              // 12: hr70.l
            vectorPath(VECTOR_F1A_PATH),                               // 13: f1a.l
            gopPath(GOP_6_PATH, 1000f, 1000f),                          // 14: b6h.z
            vectorPath(VECTOR_BME0_PATH),                              // 15: bme0.l
            vectorPath(VECTOR_LHJ_PATH),                               // 16: lhj.l
            vectorPath(VECTOR_A8J_PATH),                               // 17: a8j.l
            vectorPath(VECTOR_D06_PATH),                               // 18: d06.l
            gopPath(GOP_7_PATH, 1224.1858f, 1137.3882f),               // 19: w6b0.Io
            gopPath(GOP_5_PATH, 1000f, 1000f),                          // 20: x7l0.oOO
            vectorPath(VECTOR_IPE0_PATH),                              // 21: ipe0.l
            vectorPath(VECTOR_NSK_PATH),                               // 22: nsk.l
            vectorPath(VECTOR_PEX_PATH),                               // 23: pex.l
            gopThreeNotchShape(),                                      // 24: o7g.lO
            gopPath(GOP_9_PATH, 24f, 24f),                              // 25: x7l0.ooO
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.926f, 0.97f, 0.189f, 0.811f),
            SeedVertex(-0.021f, 0.967f, 0.187f, 0.057f)
        ), 2, 12).normalized()), // 26: qha0.III
            polygonShape(transformPolygon(regularPolygon(4, 1f, rounding = CornerRounding(0f, 0f), perVertexRounding = listOf(CornerRounding(1f, 0f), CornerRounding(1f, 0f), CornerRounding(0.2f, 0f), CornerRounding(0.2f, 0f)), centerX = 0f, centerY = 0f), rotationDegrees = 45f).normalized()), // 27: qha0.Oo
            polygonShape(rectanglePolygon(1.6f, 1f, listOf(CornerRounding(0.2f, 0f), CornerRounding(0.2f, 0f), CornerRounding(1f, 0f), CornerRounding(1f, 0f))).normalized()), // 28: qha0.Ilo
            polygonShape(transformPolygon(regularPolygon(8, 1f / cos(PI.toFloat() / 8f), rounding = CornerRounding(1f, 0f), centerX = 0f, centerY = 0f), scaleY = 0.64f, rotationDegrees = -45f).normalized()), // 29: qha0.loI
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.5f, 1.096f, 0.151f, 0.524f),
            SeedVertex(0.04f, 0.5f, 0.159f, 0.0f)
        ), 2, 12).normalized()), // 30: qha0.lOI
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.171f, 0.841f, 0.159f, 0.0f),
            SeedVertex(-0.02f, 0.5f, 0.14f, 0.0f),
            SeedVertex(0.17f, 0.159f, 0.159f, 0.0f)
        ), 2, 12).normalized()), // 31: qha0.lll
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.499f, 1.023f, 0.241f, 0.778f),
            SeedVertex(-0.005f, 0.792f, 0.208f, 0.0f),
            SeedVertex(0.073f, 0.258f, 0.228f, 0.0f),
            SeedVertex(0.433f, -0.0f, 0.491f, 0.0f)
        ), 1, 4).normalized()), // 32: qha0.lOo
            polygonShape(starPolygon(8, 0.8f, CornerRounding(0.15f, 0f)).normalized()), // 33: qha0.IOI
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.5f, 1.08f, 0.085f, 0.0f),
            SeedVertex(0.358f, 0.843f, 0.085f, 0.0f)
        ), 8, 12).normalized()), // 34: qha0.IOO
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(1.237f, 1.236f, 0.258f, 0.0f),
            SeedVertex(0.5f, 0.918f, 0.233f, 0.0f)
        ), 4, 12).normalized()), // 35: qha0.lII
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.723f, 0.884f, 0.394f, 0.0f),
            SeedVertex(0.5f, 1.099f, 0.398f, 0.0f)
        ), 6, 12).normalized()), // 36: qha0.lIO
            polygonShape(transformPolygon(starPolygon(9, 0.8f, CornerRounding(0.5f, 0f)), rotationDegrees = 90f).normalized()), // 37: qha0.lOl
            polygonShape(transformPolygon(starPolygon(12, 0.8f, CornerRounding(0.5f, 0f)), rotationDegrees = 90f).normalized()), // 38: qha0.lIl
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.5f, 0.0f, 1.0f, 0.0f),
            SeedVertex(1.0f, 0.0f, 1.0f, 0.0f),
            SeedVertex(1.0f, 1.14f, 0.254f, 0.106f),
            SeedVertex(0.575f, 0.906f, 0.253f, 0.0f)
        ), 1, 4).normalized()), // 39: qha0.lol
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.5f, 0.074f, 0.0f, 0.0f),
            SeedVertex(0.725f, -0.099f, 0.476f, 0.0f)
        ), 4, 4).normalized()), // 40: qha0.llI
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.5f, 0.036f, 0.0f, 0.0f),
            SeedVertex(0.758f, -0.101f, 0.209f, 0.0f)
        ), 8, 12).normalized()), // 41: qha0.llo
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.5f, -0.006f, 0.006f, 0.0f),
            SeedVertex(0.592f, 0.158f, 0.006f, 0.0f)
        ), 12, 12).normalized()), // 42: qha0.oO
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.193f, 0.277f, 0.053f, 0.0f),
            SeedVertex(0.176f, 0.055f, 0.053f, 0.0f)
        ), 10, 12).normalized()), // 43: qha0.IIo
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.457f, 0.296f, 0.007f, 0.0f),
            SeedVertex(0.5f, -0.051f, 0.007f, 0.0f)
        ), 15, 12).normalized()), // 44: qha0.ol
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.733f, 0.454f, 0.0f, 0.0f),
            SeedVertex(0.839f, 0.437f, 0.532f, 0.0f),
            SeedVertex(0.949f, 0.449f, 0.439f, 1.0f),
            SeedVertex(0.998f, 0.478f, 0.174f, 0.0f)
        ), 16, 4).normalized()), // 45: qha0.IIO
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.37f, 0.187f, 0.0f, 0.0f),
            SeedVertex(0.416f, 0.049f, 0.381f, 0.0f),
            SeedVertex(0.479f, 0.001f, 0.095f, 0.0f)
        ), 8, 4).normalized()), // 46: qha0.lOO
            polygonShape(transformPolygon(symmetricPolygon(listOf(
            SeedVertex(0.5f, 0.053f, 0.0f, 0.0f),
            SeedVertex(0.545f, -0.04f, 0.405f, 0.0f),
            SeedVertex(0.67f, -0.035f, 0.426f, 0.0f),
            SeedVertex(0.717f, 0.066f, 0.574f, 0.0f),
            SeedVertex(0.722f, 0.128f, 0.0f, 0.0f),
            SeedVertex(0.777f, 0.002f, 0.36f, 0.0f),
            SeedVertex(0.914f, 0.149f, 0.66f, 0.0f),
            SeedVertex(0.926f, 0.289f, 0.66f, 0.0f),
            SeedVertex(0.881f, 0.346f, 0.0f, 0.0f),
            SeedVertex(0.94f, 0.344f, 0.126f, 0.0f),
            SeedVertex(1.003f, 0.437f, 0.255f, 0.0f)
        ), 2, 4), scaleY = 0.742f).normalized()), // 47: qha0.IlI
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.87f, 0.13f, 0.146f, 0.0f),
            SeedVertex(0.818f, 0.357f, 0.0f, 0.0f),
            SeedVertex(1.0f, 0.332f, 0.853f, 0.0f)
        ), 4, 4).normalized()), // 48: qha0.IlO
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.5f, 0.0f, 0.0f, 0.0f),
            SeedVertex(0.704f, 0.0f, 0.0f, 0.0f),
            SeedVertex(0.704f, 0.065f, 0.0f, 0.0f),
            SeedVertex(0.843f, 0.065f, 0.0f, 0.0f),
            SeedVertex(0.843f, 0.148f, 0.0f, 0.0f),
            SeedVertex(0.926f, 0.148f, 0.0f, 0.0f),
            SeedVertex(0.926f, 0.296f, 0.0f, 0.0f),
            SeedVertex(1.0f, 0.296f, 0.0f, 0.0f)
        ), 2, 4).normalized()), // 49: qha0.Ill
            polygonShape(symmetricPolygon(listOf(
            SeedVertex(0.796f, 0.5f, 0.0f, 0.0f),
            SeedVertex(0.853f, 0.518f, 1.0f, 0.0f),
            SeedVertex(0.992f, 0.631f, 1.0f, 0.0f),
            SeedVertex(0.968f, 1.0f, 1.0f, 0.0f)
        ), 2, 4).normalized()), // 50: qha0.oI
        ).also { check(it.size == COUNT) { "Voyage icon-shape catalog must contain exactly $COUNT entries" } }
    }

    fun shape(index: Int): Shape = all.getOrNull(index) ?: RectangleShape

    private data class SeedVertex(
        val x: Float,
        val y: Float,
        val radius: Float,
        val smoothing: Float,
    ) {
        val rounding: CornerRounding get() = CornerRounding(radius, smoothing)
    }

    /** Exact qha0.OI seed expansion: mirrored sectors when flags&8==0, rotated sectors otherwise. */
    private fun symmetricPolygon(
        seed: List<SeedVertex>,
        repetitions: Int,
        flags: Int,
    ): RoundedPolygon {
        val coordinates = ArrayList<Float>()
        val roundings = ArrayList<CornerRounding>()
        if ((flags and 8) == 0) {
            val angles = seed.map {
                Math.toDegrees(atan2((it.y - 0.5f).toDouble(), (it.x - 0.5f).toDouble()))
            }
            val radii = seed.map { hypot(it.x - 0.5f, it.y - 0.5f) }
            val sectorCount = repetitions * 2
            val step = 360.0 / sectorCount
            for (sector in 0 until sectorCount) {
                val mirror = sector % 2
                for (index in seed.indices) {
                    val sourceIndex = if (mirror == 1) seed.lastIndex - index else index
                    if (sourceIndex > 0 || mirror == 0) {
                        val reflected = if (mirror == 0) {
                            angles[sourceIndex]
                        } else {
                            2.0 * angles[0] + (step - angles[sourceIndex])
                        }
                        val angle = Math.toRadians(sector * step + reflected)
                        val radius = radii[sourceIndex]
                        coordinates += (0.5 + cos(angle) * radius).toFloat()
                        coordinates += (0.5 + sin(angle) * radius).toFloat()
                        roundings += seed[sourceIndex].rounding
                    }
                }
            }
        } else {
            for (index in 0 until seed.size * repetitions) {
                val sourceIndex = index % seed.size
                val sector = index / seed.size
                val angle = 2.0 * PI * sector / repetitions
                val dx = seed[sourceIndex].x - 0.5f
                val dy = seed[sourceIndex].y - 0.5f
                coordinates += (0.5 + dx * cos(angle) - dy * sin(angle)).toFloat()
                coordinates += (0.5 + dx * sin(angle) + dy * cos(angle)).toFloat()
                roundings += seed[sourceIndex].rounding
            }
        }
        return RoundedPolygon(
            vertices = coordinates.toFloatArray(),
            centerX = 0.5f,
            centerY = 0.5f,
            rounding = CornerRounding(0f, 0f),
            perVertexRounding = roundings,
        )
    }

    private fun regularPolygon(
        sides: Int,
        radius: Float,
        rounding: CornerRounding = CornerRounding(0f, 0f),
        perVertexRounding: List<CornerRounding>? = null,
        centerX: Float = 0f,
        centerY: Float = 0f,
    ): RoundedPolygon {
        val vertices = FloatArray(sides * 2)
        for (index in 0 until sides) {
            val angle = 2.0 * PI * index / sides
            vertices[index * 2] = centerX + cos(angle).toFloat() * radius
            vertices[index * 2 + 1] = centerY + sin(angle).toFloat() * radius
        }
        return RoundedPolygon(
            vertices = vertices,
            centerX = centerX,
            centerY = centerY,
            rounding = rounding,
            perVertexRounding = perVertexRounding,
        )
    }

    private fun rectanglePolygon(
        width: Float,
        height: Float,
        roundings: List<CornerRounding>,
    ): RoundedPolygon {
        val halfWidth = width / 2f
        val halfHeight = height / 2f
        return RoundedPolygon(
            vertices = floatArrayOf(
                halfWidth, halfHeight,
                -halfWidth, halfHeight,
                -halfWidth, -halfHeight,
                halfWidth, -halfHeight,
            ),
            centerX = 0f,
            centerY = 0f,
            rounding = CornerRounding(0f, 0f),
            perVertexRounding = roundings,
        )
    }

    private fun starPolygon(
        pointsPerRadius: Int,
        innerRadius: Float,
        rounding: CornerRounding,
    ): RoundedPolygon {
        val vertexCount = pointsPerRadius * 2
        val vertices = FloatArray(vertexCount * 2)
        for (index in 0 until vertexCount) {
            val radius = if (index % 2 == 0) 1f else innerRadius
            val angle = PI * index / pointsPerRadius
            vertices[index * 2] = cos(angle).toFloat() * radius
            vertices[index * 2 + 1] = sin(angle).toFloat() * radius
        }
        return RoundedPolygon(
            vertices = vertices,
            centerX = 0f,
            centerY = 0f,
            rounding = rounding,
        )
    }

    private fun transformPolygon(
        polygon: RoundedPolygon,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotationDegrees: Float = 0f,
    ): RoundedPolygon {
        val radians = Math.toRadians(rotationDegrees.toDouble())
        val cosine = cos(radians)
        val sine = sin(radians)
        return polygon.transformed { x, y ->
            val scaledX = x * scaleX
            val scaledY = y * scaleY
            TransformResult(
                (scaledX * cosine - scaledY * sine).toFloat(),
                (scaledX * sine + scaledY * cosine).toFloat(),
            )
        }
    }

    private fun polygonShape(polygon: RoundedPolygon): Shape = GenericShape { size: Size, _ ->
        val path = Path()
        val cubics = polygon.cubics
        if (cubics.isNotEmpty()) {
            val first = cubics.first()
            path.moveTo(first.anchor0X * size.width, first.anchor0Y * size.height)
            cubics.forEach { cubic ->
                path.cubicTo(
                    cubic.control0X * size.width,
                    cubic.control0Y * size.height,
                    cubic.control1X * size.width,
                    cubic.control1Y * size.height,
                    cubic.anchor1X * size.width,
                    cubic.anchor1Y * size.height,
                )
            }
            path.close()
        }
        addPath(path)
    }

    /** Voyage's gop shapes scale from their source viewport without translating the path. */
    private fun gopPath(pathData: String, viewportWidth: Float, viewportHeight: Float): Shape =
        GenericShape { size: Size, _ ->
            val path = PathParser.createPathFromPathData(pathData) ?: return@GenericShape
            path.transform(Matrix().apply {
                setScale(size.width / viewportWidth, size.height / viewportHeight)
            })
            addPath(path.asComposePath())
        }

    /** Voyage's vox shapes scale by the parsed path's own bounds, matching vox.l(). */
    private fun vectorPath(pathData: String): Shape = GenericShape { size: Size, _ ->
        val path = PathParser.createPathFromPathData(pathData) ?: return@GenericShape
        val bounds = RectF()
        path.computeBounds(bounds, true)
        if (bounds.width() <= 0f || bounds.height() <= 0f) return@GenericShape
        path.transform(Matrix().apply {
            setScale(size.width / bounds.width(), size.height / bounds.height())
        })
        addPath(path.asComposePath())
    }

    /** Exact dynamic-size path used by gop case 3 (o7g.lO). */
    private fun gopThreeNotchShape(): Shape = GenericShape { size: Size, _ ->
        val width = size.width
        val height = size.height
        val centerX = width * 0.5f
        val topY = height * 0.25f
        val topControlY = height * 0.225f
        val insetY = height * 0.125f
        val sideX = width * 0.041666668f
        val sideControlY = height * 0.4f
        val lowerControlY = height * 0.5833333f
        val lowerY = height * 0.76666665f
        val bottomY = height * 0.9166667f
        val rightX = width * 0.9583333f
        moveTo(centerX, topY)
        cubicTo(centerX, topControlY, width * 0.45833334f, insetY, width * 0.29166666f, insetY)
        cubicTo(sideX, insetY, sideX, sideControlY, sideX, sideControlY)
        cubicTo(sideX, lowerControlY, width * 0.20833333f, lowerY, centerX, bottomY)
        cubicTo(width * 0.7916667f, lowerY, rightX, lowerControlY, rightX, sideControlY)
        cubicTo(rightX, sideControlY, rightX, insetY, width * 0.7083333f, insetY)
        cubicTo(width * 0.5833333f, insetY, centerX, topControlY, centerX, topY)
        close()
    }
}