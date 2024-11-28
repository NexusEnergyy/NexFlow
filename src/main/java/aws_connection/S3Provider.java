package aws_connection;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;

public class S3Provider {

    // Cria e Configura um Cliente em S3 usando as credenciais padrões
    public S3Client getS3Client() {
        return S3Client.builder()
                .region(Region.US_EAST_1)  // Região desejada
                .credentialsProvider(DefaultCredentialsProvider.create())  // Usa as credenciais padrão
                .build();
    }

    // Função para baixar arquivo do S3
    public static InputStream baixarArquivoS3(S3Client s3Client, String bucketName, String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3Client.getObject(request);
    }
}
