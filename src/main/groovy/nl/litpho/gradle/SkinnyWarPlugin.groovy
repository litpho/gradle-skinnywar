package nl.litpho.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.plugins.ear.EarPlugin
import org.gradle.api.plugins.*

/**
 * @author Jasper de Vries
 */
class SkinnyWarPlugin implements Plugin<Project> {

    static final String EARLIB_FROM_WAR_CONFIGURATION_NAME = "earlibFromWar";

    @Override
    void apply(final Project project) {
        configureConfiguration(project.configurations)

        project.afterEvaluate {
            def warlib = project.configurations.getByName(EARLIB_FROM_WAR_CONFIGURATION_NAME)

            getWarProjects(project).each { warProject ->
                def classpathHolder = []

                // Add non-jar classpath items to classpathHolder
                warProject.tasks['war'].classpath.each { file ->
                    if (!file.path.endsWith('.jar')) {
                        classpathHolder << file
                    }
                }

                // Copies to prevent resolving the runtime/earlib base configurations
                def warConfig = warProject.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME).copyRecursive()

                // Copy the excludeRules to the earlib_from_war configuration
                warConfig.excludeRules*.each {
                    warlib.exclude(group: it.group, module: it.module)
                }

                def earArtifacts = getArtifactList(project, EarPlugin.EARLIB_CONFIGURATION_NAME)
                def providedCompileArtifacts = getArtifactList(warProject, WarPlugin.PROVIDED_COMPILE_CONFIGURATION_NAME)

                warConfig.resolvedConfiguration.resolvedArtifacts.each { warDependency ->
                    def warId = warDependency.moduleVersion.id

                    if (providedCompileArtifacts.contains("$warId.group:$warId.module")) {
                        // Never add providedCompile artifacts to either EAR or WAR
                        project.logger.info "Removing war dependency $warId"
                    } else {
                        if (!earArtifacts.contains("$warId.group:$warId.module")) {
                            project.logger.info "Putting back $warId.group:$warId.name:$warId.version"
                            classpathHolder << warDependency.file
                        } else {
                            project.logger.info "Removing war dependency $warId"

                            project.dependencies.add(EARLIB_FROM_WAR_CONFIGURATION_NAME, getDependencyToAdd(project, warId))
                        }
                    }
                }

                // Reset war classpath to only the listed files/folders
                warProject.tasks['war'].classpath = classpathHolder

                // Filter the modules remaining in the war when releasing
                project.getGradle().getTaskGraph().whenReady { graph ->
                    def releaseVersion = (String) project.properties['releaseVersion']
                    if (graph.hasTask(':unSnapshotVersion')) {
                        def tempClasspath = []
                        warProject.tasks['war'].classpath.each { File file ->
                            def projectVersion = (String) project.version
                            if (file.path.startsWith(project.rootDir.path) && file.path.endsWith(projectVersion + '.jar')) {
                                project.logger.info "Removing snapshot from $file.path for release"
                                tempClasspath << new File(file.path.replace(projectVersion, releaseVersion))
                            } else {
                                tempClasspath << file
                            }
                        }

                        warProject.tasks['war'].classpath = tempClasspath
                    }
                }
            }
        }
    }

    private static void configureConfiguration(final ConfigurationContainer configurationContainer) {
        Configuration earlibFromWarConfiguration = configurationContainer.create(EARLIB_FROM_WAR_CONFIGURATION_NAME).setVisible(false).
                setDescription('Additional earlib classpath for libraries that should not be part of the WAR archive and should resolve in the EAR.');
        configurationContainer.getByName(EarPlugin.EARLIB_CONFIGURATION_NAME).extendsFrom(earlibFromWarConfiguration);
    }

    private static List<String> getArtifactList(final Project project, final String configurationName) {
        def configuration = project.configurations.getByName(configurationName).copyRecursive()

        configuration.resolvedConfiguration.resolvedArtifacts.collect() { "$it.moduleVersion.id.group:$it.moduleVersion.id.module" }
    }

    private static List<Project> getWarProjects(final Project project) {
        def list = []

        project.configurations.getByName(EarPlugin.DEPLOY_CONFIGURATION_NAME).allDependencies.withType(ProjectDependency.class) { dependency ->
            if (dependency.dependencyProject.plugins.hasPlugin('war')) {
                list << dependency.dependencyProject
            }
        }

        return list
    }

    static Object getDependencyToAdd(final Project project, final ModuleVersionIdentifier moduleVersionIdentifier) {
        final String rootProjectName = project.rootProject.name
        if (moduleVersionIdentifier.group.startsWith(rootProjectName)) {
        	final String moduleWithoutProjectName
        	if (rootProjectName.length() == moduleVersionIdentifier.group.length()) {
            	moduleWithoutProjectName = ''
            } else {
            	moduleWithoutProjectName = ':' + moduleVersionIdentifier.group.substring(rootProjectName.length() + 1).replace('.', ':')
            }
            return project.findProject("$moduleWithoutProjectName:$moduleVersionIdentifier.name")
        } else {
            return "$moduleVersionIdentifier.group:$moduleVersionIdentifier.name:$moduleVersionIdentifier.version"
        }
    }
}
