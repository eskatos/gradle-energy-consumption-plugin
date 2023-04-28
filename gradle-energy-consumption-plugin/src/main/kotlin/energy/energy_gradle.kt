package energy

import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.internal.operations.*
import org.gradle.kotlin.dsl.withGroovyBuilder


abstract class EnergyMonitorService : BuildService<BuildServiceParameters.None>,
    BuildOperationListener,
    AutoCloseable {

    companion object {
        internal
        val logger = Logging.getLogger(EnergyMonitorService::class.java)
    }

    private val energyMonitor = EnergyMonitor()
    private val energyMonitorThread = Thread(energyMonitor, "Energy Monitor")

    init {
        energyMonitorThread.start()
        logger.info("Energy Monitor STARTED")
    }

    val processConsumedWh: Double
        get() = energyMonitor.processConsumedJoules / 3600

    val systemConsumedWh: Double
        get() = energyMonitor.systemConsumedJoules / 3600

    override fun close() {
        logger.info("Energy Monitor STOPPED")
        energyMonitorThread.interrupt()
    }

    override fun started(buildOperation: BuildOperationDescriptor, startEvent: OperationStartEvent) = Unit
    override fun progress(operationIdentifier: OperationIdentifier, progressEvent: OperationProgressEvent) = Unit
    override fun finished(buildOperation: BuildOperationDescriptor, finishEvent: OperationFinishEvent) = Unit
}


abstract class ReportEnergyConsumption : FlowAction<ReportEnergyConsumption.Params> {

    interface Params : FlowParameters {

        @get:Input
        val buildFinished: Property<Unit>

        @get:Optional
        @get:Input
        val buildScanExtension: Property<Any>

        @get:ServiceReference
        val energyMonitorService: Property<EnergyMonitorService>
    }

    override fun execute(parameters: Params) {
        val monitorService = parameters.energyMonitorService.get()
        val processConsumedWh = monitorService.processConsumedWh
        val systemConsumedWh = monitorService.systemConsumedWh
        parameters.buildScanExtension.orNull?.withGroovyBuilder {
            "value"("PWR_DAEMON_WH", String.format("%.8f", processConsumedWh))
            "value"("PWR_SYSTEM_WH", String.format("%.8f", systemConsumedWh))
        }
        EnergyMonitorService.logger.lifecycle(buildString {
            appendLine()
            appendLine("Energy consumption for this build invocation")
            append("  Gradle Daemon ")
            appendLine(String.format("%.4f Wh", processConsumedWh).padStart(16))
            append("  System ")
            appendLine(String.format("%.4f Wh", systemConsumedWh).padStart(16 + 7))
        })
    }
}
