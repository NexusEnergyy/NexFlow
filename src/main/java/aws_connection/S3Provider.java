package aws_connection;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;

public class S3Provider {

    private final S3Client s3Client;

    public S3Provider() {
        this.s3Client = createS3Client();
    }

    private S3Client createS3Client() {
        StsClient stsClient = StsClient.builder()
                .region(Region.US_EAST_1)
                .build();

        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .roleArn("arn:aws:iam::039678994289:instance-profile/LabInstanceProfile")
                .roleSessionName("sessionName")
                .build();

        AssumeRoleResponse assumeRoleResponse = stsClient.assumeRole(assumeRoleRequest);
        Credentials stsCredentials = assumeRoleResponse.credentials();

        AwsSessionCredentials awsCredentials = AwsSessionCredentials.create(
                stsCredentials.accessKeyId(),
                stsCredentials.secretAccessKey(),
                stsCredentials.sessionToken()
        );

        return S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }

    public S3Client getS3Client() {
        return s3Client;
    }

    public static InputStream baixarArquivoS3(S3Client s3Client, String bucketName, String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3Client.getObject(request);
    }
}