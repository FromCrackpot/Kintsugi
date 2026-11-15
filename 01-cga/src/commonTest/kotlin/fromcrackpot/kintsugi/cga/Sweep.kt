package fromcrackpot.kintsugi.cga

import fromcrackpot.kintsugi.cga.d2.operations.products.join
import fromcrackpot.kintsugi.cga.d2.operations.products.meet
import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Composite
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.composites.unaryMinus
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Primitive
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundDirection
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.unaryMinus
import fromcrackpot.kintsugi.cga.d2.transforms.D
import fromcrackpot.kintsugi.cga.d2.transforms.Dilator
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.G
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.d2.transforms.times
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

internal const val SEED: Int = 0x6A5
internal const val DRAWS: Int = 200
internal const val REACH: Double = 1_000.0

internal fun sweep(law: Random.() -> Unit) {
    val random = Random(SEED)
    repeat(DRAWS) { draw ->
        try {
            random.law()
        } catch (e: Throwable) {
            fail("draw $draw of seed $SEED — ${e.message}", e)
        }
    }
}

/* COMPARISON */

internal fun assertNear(expected: Double, actual: Double, message: String) {
    assertTrue(round(expected - actual) == 0.0, "$message: expected $expected, actual $actual")
}

internal fun assertSameShape(expected: Shape, actual: Shape) {
    assertEquals(expected::class, actual::class, "class of $actual against $expected")
    val e = expected.components
    val a = actual.components
    assertEquals(e.size, a.size, "component count of $actual against $expected")
    for (i in e.indices) assertNear(e[i], a[i], "component $i of $actual against $expected")
}

private val Shape.components: List<Double>
    get() = when (this) {
        is Primitive -> when (this) {
            is FlatPoint -> listOf(px, py, pz)
            is Line -> listOf(gx, gy, gz)
            is RoundPoint -> listOf(ax, ay, az, aw)
            is Dipole -> listOf(gx, gy, gz, px, py, pz)
            is Circle -> listOf(cx, cy, cz, cw)
        }
        is Composite -> when (this) {
            is Segment -> listOf(gx, gy, gz, px, py, pz)
            is Polyline -> vertices.flatMap { it.components }
            is Polygon -> vertices.flatMap { it.components }
        }
        else -> error("unknown shape category: ${this::class}")
    }

/* GENERATORS: SCALARS */

internal fun Random.degenerate(): Boolean = nextInt(10) == 0

internal fun Random.coordinate(): Double = nextDouble(-REACH, REACH)

internal fun Random.scale(): Double = 2.0.pow(nextDouble(-1.0, 1.0))

internal fun Random.angle(): Angle = Angle(nextDouble(-PI, PI))

/* GENERATORS: SHAPES */

internal fun Random.flatPoint(): FlatPoint = FlatPoint(coordinate(), coordinate())

internal fun Random.direction(): Direction = Direction(coordinate(), coordinate())

internal fun Random.roundPoint(): RoundPoint = RoundPoint(coordinate(), coordinate())

internal fun Random.nonNullRoundPoint(): RoundPoint {
    val x = coordinate()
    val y = coordinate()
    return RoundPoint(x, y, 1.0, (x * x + y * y + nextDouble(-1.0, 1.0) * REACH * REACH / 4) / 2)
}

internal fun Random.roundDirection(): RoundDirection = RoundDirection(coordinate(), coordinate(), coordinate())

internal fun Random.line(): Line = flatPoint() join flatPoint()

internal fun Random.circle(): Circle = Circle(coordinate(), coordinate(), nextDouble(1.0, REACH / 2))

internal fun Random.imaginaryCircle(): Circle {
    val x = coordinate()
    val y = coordinate()
    val r = nextDouble(1.0, REACH / 2)
    return Circle(x, y, -0.5 * (x * x + y * y + r * r), -1.0)
}

internal fun Random.realDipole(): Dipole = roundPoint() join roundPoint()

internal fun Random.imaginaryDipole(): Dipole {
    val a = flatPoint()
    val b = flatPoint()
    val span = hypot(b.px - a.px, b.py - a.py)
    return Circle(a.px, a.py, span * nextDouble(0.05, 0.45)) meet Circle(b.px, b.py, span * nextDouble(0.05, 0.45))
}

internal fun Random.dipole(): Dipole = if (nextBoolean()) realDipole() else imaginaryDipole()

internal fun Random.segment(): Segment = Segment(flatPoint(), flatPoint())

internal fun Random.polyline(): Polyline = Polyline(List(nextInt(2, 7)) { flatPoint() })

internal fun Random.polygon(): Polygon {
    val center = flatPoint()
    val n = nextInt(4, 9)
    val loop = Polygon(List(n) { k ->
        val theta = (k + nextDouble(-0.25, 0.25)) * 2.0 * PI / n
        val r = nextDouble(20.0, 300.0)
        FlatPoint(center.px + r * cos(theta), center.py + r * sin(theta))
    })
    return if (nextBoolean()) -loop else loop
}

internal fun Random.convexPolygon(center: FlatPoint, radius: Double): Polygon {
    val n = nextInt(3, 9)
    val phase = nextDouble(0.0, 2.0 * PI)
    return Polygon(List(n) { k ->
        val theta = phase + (k + nextDouble(-0.25, 0.25)) * 2.0 * PI / n
        FlatPoint(center.px + radius * cos(theta), center.py + radius * sin(theta))
    })
}

internal fun Random.shape(): Shape = when (nextInt(13)) {
    0 -> flatPoint()
    1 -> direction()
    2 -> roundPoint()
    3 -> nonNullRoundPoint()
    4 -> roundDirection()
    5 -> line()
    6 -> circle()
    7 -> -circle()
    8 -> imaginaryCircle()
    9 -> dipole()
    10 -> segment()
    11 -> polyline()
    else -> polygon()
}

internal fun Random.subjectAndRegion(): Pair<Polygon, Polygon> {
    val reach = nextDouble(50.0, 400.0)
    val region = convexPolygon(FlatPoint(coordinate() / 2, coordinate() / 2), reach)
    if (degenerate()) return touching(region) to region
    val radius = nextDouble(10.0, 150.0)
    val offset = reach + radius
    val c = region.centroid
    return convexPolygon(FlatPoint(c.px + nextDouble(-offset, offset), c.py + nextDouble(-offset, offset)), radius) to region
}

private fun Random.touching(region: Polygon): Polygon {
    val i = nextInt(region.vertices.size)
    val p = region.vertices[i]
    val q = region.vertices[(i + 1) % region.vertices.size]
    val length = hypot(q.px - p.px, q.py - p.py)
    val ux = (q.px - p.px) / length
    val uy = (q.py - p.py) / length
    val t = nextDouble(0.1, 0.9) * length
    val mx = p.px + t * ux
    val my = p.py + t * uy
    val h = nextDouble(5.0, 100.0)
    val w = nextDouble(5.0, 100.0)
    return Polygon(
        listOf(
            FlatPoint(mx, my),
            FlatPoint(mx + h * uy - w * ux, my - h * ux - w * uy),
            FlatPoint(mx + h * uy + w * ux, my - h * ux + w * uy),
        )
    )
}

/* GENERATORS: MOTIONS */

internal fun Random.motor(): Motor = R(flatPoint(), angle()) * T(coordinate() / 2, coordinate() / 2)

internal fun Random.flector(): Flector = if (nextBoolean()) F(line()) else G(line(), coordinate() / 2)

internal fun Random.versor(): Versor = if (nextBoolean()) motor() else flector()

internal fun Random.dilator(): Dilator = D(flatPoint(), scale())
