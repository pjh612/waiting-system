CREATE TABLE "client" (
                          "ID" BIGINT AUTO_INCREMENT PRIMARY KEY,
                          "CLIENT_ID" VARCHAR(50),
                          "NAME" VARCHAR(100),
                          "SECRET" VARCHAR(100),
                          "CREATED_AT" TIMESTAMP
);
CREATE TABLE "waiting_queue" (
                                 ID BIGINT AUTO_INCREMENT PRIMARY KEY,

                                 QUEUE_ID BINARY(16) NOT NULL,
                                 CLIENT_ID BINARY(16) NOT NULL,

                                 NAME VARCHAR(255) NOT NULL,
                                 REDIRECT_URL VARCHAR(2083) NOT NULL,
                                 SIZE BIGINT NOT NULL,
                                 API_KEY VARCHAR(255) NOT NULL,
                                 SECRET VARCHAR(255) NOT NULL,

                                 CREATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  );

CREATE INDEX IDX_WAITING_QUEUE_CLIENT_ID
    ON "waiting_queue" (CLIENT_ID);
