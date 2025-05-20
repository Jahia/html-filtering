import org.jahia.osgi.BundleUtils
import org.jahia.services.modulemanager.spi.Config
import org.jahia.services.modulemanager.spi.ConfigService

ConfigService configService = BundleUtils.getOsgiService(ConfigService.class, null)
def pid = "PID"
def identifier = "IDENTIFIER"
Config conf
if ("".equals(identifier)) {
    log.info("Deleting config by pid '{}'", pid)
    conf = configService.getConfig(pid)
} else {
    log.info("Deleting config by pid '{}' and identifier '{}'", pid, identifier)
    conf = configService.getConfig(pid, identifier)
}
def filename = conf.getIdentifier() // conf.getProperties().get("felix.fileinstall.filename");
configService.deleteConfig(conf)
log.info("Config {} deleted", filename)
