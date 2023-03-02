# Configuration Server

## Start with 'dev' profile

- Set variables in '$USER_HOME/dev.env'
```properties
# SSH git URL of config repository, for example: ssh://git@git.domain.corp/CMN/service-config.git
SPRING_CLOUD_CONFIG_SERVER_GIT_URI=
# Passhphrase of developer's private key. Public key must be added to VCS for config repository access to via SSH
SPRING_CLOUD_CONFIG_SERVER_GIT_PASSPHRASE=
# Hashicorp Vault hostname, e.g. vault.domain.corp
SPRING_CLOUD_CONFIG_SERVER_VAULT_HOST=
```
- Run ConfigServerApplication (dev) via IntelliJ Idea or launch via shell

```shell
./gradlew bootRun --args='--spring.profiles.active=dev --spring.config.additional-location=dev/'
```
