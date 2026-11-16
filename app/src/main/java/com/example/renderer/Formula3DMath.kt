package com.example.renderer

import com.example.model.Vector3D
import kotlin.math.*

data class Point2D(val x: Float, val y: Float, val depth: Float)

data class CamPoint3D(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f)

class Camera3D(
    var position: Vector3D = Vector3D(0f, -6f, 2.2f),
    var target: Vector3D = Vector3D(0f, 10f, 1f),
    var up: Vector3D = Vector3D(0f, 0f, 1f),
    var fovDegrees: Float = 65f
) {
    var forward = Vector3D(0f, 1f, 0f)
    var right = Vector3D(1f, 0f, 0f)
    var trueUp = Vector3D(0f, 0f, 1f)

    private var cachedTanHalfFov = tan((65f * PI.toFloat() / 180f) * 0.5f)
    private var cachedFov = 65f

    fun updateVectors() {
        val dx = target.x - position.x
        val dy = target.y - position.y
        val dz = target.z - position.z
        val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.001f)
        forward.x = dx / len
        forward.y = dy / len
        forward.z = dz / len

        // Right = Forward x Up
        val rx = forward.y * up.z - forward.z * up.y
        val ry = forward.z * up.x - forward.x * up.z
        val rz = forward.x * up.y - forward.y * up.x
        val rLen = sqrt(rx * rx + ry * ry + rz * rz).coerceAtLeast(0.001f)
        right.x = rx / rLen
        right.y = ry / rLen
        right.z = rz / rLen

        // TrueUp = Right x Forward
        val ux = right.y * forward.z - right.z * forward.y
        val uy = right.z * forward.x - right.x * forward.z
        val uz = right.x * forward.y - right.y * forward.x
        val uLen = sqrt(ux * ux + uy * uy + uz * uz).coerceAtLeast(0.001f)
        trueUp.x = ux / uLen
        trueUp.y = uy / uLen
        trueUp.z = uz / uLen

        if (abs(fovDegrees - cachedFov) > 0.01f) {
            cachedFov = fovDegrees
            val fovRad = (fovDegrees * PI.toFloat()) / 180f
            cachedTanHalfFov = tan(fovRad * 0.5f).coerceAtLeast(0.05f)
        }
    }

    fun toCameraSpace(wx: Float, wy: Float, wz: Float, out: CamPoint3D) {
        val relX = wx - position.x
        val relY = wy - position.y
        val relZ = wz - position.z
        out.x = relX * right.x + relY * right.y + relZ * right.z
        out.y = relX * trueUp.x + relY * trueUp.y + relZ * trueUp.z
        out.z = relX * forward.x + relY * forward.y + relZ * forward.z
    }

    fun projectCameraSpace(
        camX: Float,
        camY: Float,
        camZ: Float,
        screenWidth: Float,
        screenHeight: Float,
        nearPlane: Float = 0.35f
    ): Point2D? {
        if (camZ < nearPlane) return null
        val aspect = screenWidth / screenHeight.coerceAtLeast(1f)
        val projX = camX / (camZ * cachedTanHalfFov * aspect)
        val projY = camY / (camZ * cachedTanHalfFov)
        val screenX = (projX + 1f) * 0.5f * screenWidth
        val screenY = (1f - projY) * 0.5f * screenHeight
        return Point2D(screenX, screenY, camZ)
    }

    /**
     * Zero-allocation projection into caller-provided FloatArray [x, y, depth].
     * Returns true if in front of nearPlane.
     */
    fun projectFast(
        wx: Float,
        wy: Float,
        wz: Float,
        screenWidth: Float,
        screenHeight: Float,
        outXYD: FloatArray,
        nearPlane: Float = 0.35f
    ): Boolean {
        val relX = wx - position.x
        val relY = wy - position.y
        val relZ = wz - position.z

        val camZ = relX * forward.x + relY * forward.y + relZ * forward.z
        if (camZ < nearPlane) return false

        val camX = relX * right.x + relY * right.y + relZ * right.z
        val camY = relX * trueUp.x + relY * trueUp.y + relZ * trueUp.z

        val aspect = screenWidth / screenHeight.coerceAtLeast(1f)
        val projX = camX / (camZ * cachedTanHalfFov * aspect)
        val projY = camY / (camZ * cachedTanHalfFov)

        outXYD[0] = (projX + 1f) * 0.5f * screenWidth
        outXYD[1] = (1f - projY) * 0.5f * screenHeight
        outXYD[2] = camZ
        return true
    }

    /**
     * Projects a 3D point in world space into 2D screen coordinates.
     * Returns null if point is behind camera plane.
     */
    fun project(point: Vector3D, screenWidth: Float, screenHeight: Float): Point2D? {
        val relX = point.x - position.x
        val relY = point.y - position.y
        val relZ = point.z - position.z

        val camZ = relX * forward.x + relY * forward.y + relZ * forward.z
        if (camZ < 0.35f) return null // Near plane clip

        val camX = relX * right.x + relY * right.y + relZ * right.z
        val camY = relX * trueUp.x + relY * trueUp.y + relZ * trueUp.z

        val aspect = screenWidth / screenHeight.coerceAtLeast(1f)
        val projX = camX / (camZ * cachedTanHalfFov * aspect)
        val projY = camY / (camZ * cachedTanHalfFov)

        val screenX = (projX + 1f) * 0.5f * screenWidth
        val screenY = (1f - projY) * 0.5f * screenHeight

        return Point2D(screenX, screenY, camZ)
    }

    /**
     * Clips a camera-space polygon (up to maxVerts) against the near plane z = nearZ
     * using Sutherland-Hodgman clipping, writing projected screen coordinates into outScreenXY.
     * Returns number of clipped vertices (>= 3 means visible polygon).
     */
    fun clipAndProjectPolygon(
        inCamVerts: Array<CamPoint3D>,
        vertCount: Int,
        screenWidth: Float,
        screenHeight: Float,
        outCamClipped: Array<CamPoint3D>,
        outScreenX: FloatArray,
        outScreenY: FloatArray,
        nearZ: Float = 0.45f
    ): Int {
        var outCount = 0
        val maxOut = outCamClipped.size
        for (i in 0 until vertCount) {
            val a = inCamVerts[i]
            val b = inCamVerts[(i + 1) % vertCount]
            val aIn = a.z >= nearZ
            val bIn = b.z >= nearZ

            if (aIn && bIn) {
                if (outCount < maxOut) {
                    outCamClipped[outCount].x = b.x
                    outCamClipped[outCount].y = b.y
                    outCamClipped[outCount].z = b.z
                    outCount++
                }
            } else if (aIn && !bIn) {
                val t = ((nearZ - a.z) / (b.z - a.z)).coerceIn(0f, 1f)
                if (outCount < maxOut) {
                    outCamClipped[outCount].x = a.x + (b.x - a.x) * t
                    outCamClipped[outCount].y = a.y + (b.y - a.y) * t
                    outCamClipped[outCount].z = nearZ
                    outCount++
                }
            } else if (!aIn && bIn) {
                val t = ((nearZ - a.z) / (b.z - a.z)).coerceIn(0f, 1f)
                if (outCount < maxOut) {
                    outCamClipped[outCount].x = a.x + (b.x - a.x) * t
                    outCamClipped[outCount].y = a.y + (b.y - a.y) * t
                    outCamClipped[outCount].z = nearZ
                    outCount++
                }
                if (outCount < maxOut) {
                    outCamClipped[outCount].x = b.x
                    outCamClipped[outCount].y = b.y
                    outCamClipped[outCount].z = b.z
                    outCount++
                }
            }
        }

        if (outCount < 3) return 0

        val aspect = screenWidth / screenHeight.coerceAtLeast(1f)
        val invTan = 1f / cachedTanHalfFov
        val invTanAspect = invTan / aspect

        for (i in 0 until outCount) {
            val v = outCamClipped[i]
            val invZ = 1f / v.z
            val projX = v.x * invZ * invTanAspect
            val projY = v.y * invZ * invTan
            outScreenX[i] = (projX + 1f) * 0.5f * screenWidth
            outScreenY[i] = (1f - projY) * 0.5f * screenHeight
        }
        return outCount
    }
}

