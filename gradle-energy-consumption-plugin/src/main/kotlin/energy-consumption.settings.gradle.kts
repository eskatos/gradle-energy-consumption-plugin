import energy.EnergyMonitorService
import energy.ReportEnergyConsumption
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal
import org.gradle.kotlin.dsl.support.serviceOf

val flowScope = serviceOf<FlowScope>()
val flowProviders = serviceOf<FlowProviders>()
val buildEventsListenerRegistry = serviceOf<BuildEventListenerRegistryInternal>()

val monitorServiceProvider = gradle.sharedServices.registerIfAbsent(
    "energyMonitorService",
    EnergyMonitorService::class
) {}

buildEventsListenerRegistry.onOperationCompletion(monitorServiceProvider)

gradle.settingsEvaluated {
    flowScope.always(ReportEnergyConsumption::class) {
        parameters.buildFinished.set(flowProviders.buildWorkResult.map { })
        if (pluginManager.hasPlugin("com.gradle.enterprise")) {
            parameters.buildScanExtension.set(settings.buildScanExtension)
        }
    }
}

val Settings.buildScanExtension: Any
    get() = withGroovyBuilder {
        getProperty("gradleEnterprise").withGroovyBuilder {
            getProperty("buildScan")
        }
    }
