package nl.litpho.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.logging.Logger
import spock.lang.Specification

/**
 * @author Jasper de Vries
 */
class SkinnyWarPluginTest extends Specification {

    SkinnyWarPlugin fixture = new SkinnyWarPlugin()
    Project rootProject = Mock(Project)
    Project project = Mock(Project)
    Project target = Mock(Project)
    Logger logger = Mock(Logger)

    def "testGroupThreeLevelsDeep"() {
        when:
        ModuleVersionIdentifier moduleVersionIdentifier =
                new DefaultModuleVersionIdentifier('myrootproject.domain.common', 'common-types', '15.0.0-SNAPSHOT')

        rootProject.getName() >> 'myrootproject'
        project.getRootProject() >> rootProject
        1 * project.findProject(':domain:common:common-types') >> target
        project.logger >> logger
        logger.lifecycle(_) >> { String message -> println message }

        def result = fixture.getDependencyToAdd(project, moduleVersionIdentifier)

        then:
        result == target
    }
}
