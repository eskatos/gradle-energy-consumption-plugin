import energy.EnergyMonitorService
import energy.ReportEnergyConsumption
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal
import org.gradle.kotlin.dsl.support.serviceOf

val flowScope = serviceOf<FlowScope>()
val flowProviders = serviceOf<FlowProviders>()
val buildEventsListenerRegistry = serviceOf<BuildEventListenerRegistryInternal>()

val monitorService = gradle.sharedServices.registerIfAbsent("energyMonitorService", EnergyMonitorService::class) {}

flowScope.always(ReportEnergyConsumption::class) {
    parameters.apply {
        energyMonitorService.set(monitorService)
        buildFinished.set(flowProviders.buildWorkResult.map { })
    }
}

buildEventsListenerRegistry.onOperationCompletion(monitorService)
