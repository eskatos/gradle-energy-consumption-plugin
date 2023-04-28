package energy

import com.sun.management.OperatingSystemMXBean
import org.gradle.api.logging.Logging
import java.lang.management.ManagementFactory
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.readText

class EnergyMonitor : Runnable {

    var systemConsumedJoules: Double = 0.0

    var processConsumedJoules: Double = 0.0

    private val os: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
    private val linuxEnergy = LinuxEnergy()

    init {
        ensureReady()
    }

    override fun run() {
        var lastJoules = linuxEnergy.cpuConsumedJoules()
        while (!Thread.currentThread().isInterrupted) {
            val joules = linuxEnergy.cpuConsumedJoules()
            val energy = joules - lastJoules
            val processCpuLoad = os.processCpuLoad
            val jvmEnergy = os.cpuLoad.takeIf { it > 0 }
                ?.let { cpuLoad -> (processCpuLoad * energy) / cpuLoad }
                ?: (processCpuLoad * energy)
            processConsumedJoules += jvmEnergy
            systemConsumedJoules += energy
            lastJoules = joules
            sleep()
        }
    }

    private fun sleep() {
        try {
            Thread.sleep(250)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun ensureReady() {
        val start = System.currentTimeMillis()
        while (os.cpuLoad < 0 && os.processCpuLoad < 0) {
            sleep()
            if (System.currentTimeMillis() > (start + 2000)) {
                throw IllegalStateException("OS MX Bean provides wrong values")
            }
        }
    }
}

private class LinuxEnergy {

    companion object {
        private const val RAPL_PSYS = "/sys/class/powercap/intel-rapl/intel-rapl:1/energy_uj"
        private const val RAPL_PKG = "/sys/class/powercap/intel-rapl/intel-rapl:0/energy_uj"
        private const val RAPL_DRAM = "/sys/class/powercap/intel-rapl/intel-rapl:0/intel-rapl:0:2/energy_uj"

        private val LOGGER = Logging.getLogger(LinuxEnergy::class.java)
    }

    private val raplSources: List<Path> = buildList {
        val fs = FileSystems.getDefault()
        val psys = fs.getPath(RAPL_PSYS)
        if (psys.toFile().canRead()) {
            add(psys)
        } else {
            val pkg = fs.getPath(RAPL_PKG)
            if (psys.toFile().canRead()) {
                add(pkg)
                val dram = fs.getPath(RAPL_DRAM)
                if (psys.toFile().canRead()) {
                    add(dram)
                }
            }
        }
    }

    init {
        require(raplSources.isNotEmpty()) {
            "No readable RAPL source found, check permissions of $RAPL_PSYS, $RAPL_PKG and $RAPL_DRAM"
        }
        LOGGER.info("RAPL sources: ${raplSources.joinToString(", ")}")
    }

    fun cpuConsumedJoules(): Double {
        return raplSources.sumOf { it.readText().toDouble() } / 1_000_000
    }
}
