package com.intridea.io.vfs.support;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.github.vfss3.S3FileSystemOptions;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import static com.amazonaws.SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.SECRET_KEY_ENV_VAR;
import static java.lang.Boolean.parseBoolean;

/**
 * @author <A href="mailto:alexey at abashev dot ru">Alexey Abashev</A>
 */
public abstract class AbstractS3FileSystemTest {
    protected final Logger log = LoggerFactory.getLogger(AbstractS3FileSystemTest.class);

    private static final String BASE_URL = "BASE_URL";
    private static final String USE_HTTP = "USE_HTTPS";
    private static final String CREATE_BUCKET = "CREATE_BUCKET";
    private static final String DISABLE_CHUNKED_ENCODING = "DISABLE_CHUNKED_ENCODING";

    protected FileSystemManager vfs;
    protected S3FileSystemOptions options;
    protected String baseUrl;

    @BeforeClass
    public final void initVFS() throws IOException {
        this.options = new S3FileSystemOptions();

        EnvironmentConfiguration configuration = new EnvironmentConfiguration();

        // Try to load access and secret key from environment
        AWSCredentials awsCredentials = null;

        try {
            awsCredentials = (new EnvironmentVariableCredentialsProvider()).getCredentials();
        } catch (AmazonClientException e) {
            log.info("Not able to load credentials from environment - try .envrc file");
        }

        if (awsCredentials != null) {
            log.info("Will use AWS credentials from environment variables");
        } else {
            configuration.computeIfPresent(
                    ACCESS_KEY_ENV_VAR,
                    SECRET_KEY_ENV_VAR,
                    (access, secret) -> options.setCredentialsProvider(new AWSStaticCredentialsProvider(new BasicAWSCredentials(access, secret))));
        }

        configuration.computeIfPresent(USE_HTTP, v -> options.setUseHttps(parseBoolean(v)));
        configuration.computeIfPresent(CREATE_BUCKET, v -> options.setCreateBucket(parseBoolean(v)));
        configuration.computeIfPresent(DISABLE_CHUNKED_ENCODING, v -> options.setDisableChunkedEncoding(parseBoolean(v)));

        baseUrl = configuration.get(BASE_URL).
                orElseThrow(() -> new IllegalStateException(BASE_URL + " should present in environment configuration"));

        this.vfs = VFS.getManager();
    }

    public FileObject resolveFile(String path) throws FileSystemException {
        return vfs.resolveFile(baseUrl + path, options.toFileSystemOptions());
    }

    public FileObject resolveFile(String path, Consumer<S3FileSystemOptions> optionsConsumer) throws FileSystemException {
        S3FileSystemOptions newOptions = new S3FileSystemOptions(options.toFileSystemOptions());

        optionsConsumer.accept(newOptions);

        return vfs.resolveFile(baseUrl + path, newOptions.toFileSystemOptions());
    }

    /**
     * Local binary file for doing tests.
     *
     * @return
     */
    public File binaryFile() {
        return new File("src/test/resources/backup.zip");
    }

    /**
     * Returns path to big file for doing upload test.
     *
     * @return
     */
    public String bigFile() {
        return "http://archive.ubuntu.com/ubuntu/dists/xenial-updates/main/installer-i386/current/images/netboot/mini.iso";
    }
}
