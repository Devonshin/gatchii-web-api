package shared.postgres

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * @author Devonshin
 * @date 2025-09-14
 */
object PostgresTestContainer {
  // 고정 이미지 사용으로 재현성 확보
  private val image: DockerImageName = DockerImageName.parse("postgres:16.4-alpine")
    .asCompatibleSubstituteFor("postgres")

  // 단일 컨테이너 인스턴스 재사용
  val container: PostgreSQLContainer<*> = PostgreSQLContainer(image)
    .withDatabaseName("gatchii-test")
    .withUsername("test")
    .withPassword("test")
    .withReuse(true)
}