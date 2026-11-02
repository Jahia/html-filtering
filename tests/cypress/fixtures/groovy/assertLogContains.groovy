import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.RollingFileAppender

// Fails (the provisioning result is then '.failed') when the end of jahia.log does not contain the expected text
def expectedText = "EXPECTED_TEXT"
def maxBytesToRead = 5 * 1024 * 1024

def logFile = ((LoggerContext) LogManager.getContext(false)).getConfiguration().getAppenders().values()
        .findAll { it instanceof RollingFileAppender }
        .collect { new File(((RollingFileAppender) it).getFileName()) }
        .find { it.name == "jahia.log" }
if (logFile == null) {
    logFile = new File(System.getProperty("jahia.log.dir"), "jahia.log")
}

def raf = new RandomAccessFile(logFile, "r")
try {
    long start = Math.max(0L, raf.length() - maxBytesToRead)
    byte[] bytes = new byte[(int) (raf.length() - start)]
    raf.seek(start)
    raf.readFully(bytes)
    if (!new String(bytes, "UTF-8").contains(expectedText)) {
        throw new IllegalStateException("The log " + logFile + " does not contain: " + expectedText)
    }
    log.info("The log {} contains the expected text", logFile)
} finally {
    raf.close()
}
