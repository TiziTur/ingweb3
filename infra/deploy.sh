#!/bin/bash
# =============================================================================
# Deploy del backend en la VM del grupo.
#
# Uso (en la VM):   ~/infra/deploy.sh
#
# Qué hace:
#   1. Clona la rama main del repo del grupo en /tmp
#   2. Compila el .war con Maven dentro de un contenedor Docker (no hace falta Maven en la VM)
#   3. Reemplaza la aplicación en Tomcat (~/infra/tomcat/webapps/ROOT) y reinicia el backend
#
# Es la misma lógica que el deploy.sh del profesor, con dos cambios:
#   - clona el repo del grupo en lugar de magm3333/iw3_2026
#   - copia el .war con "cp" en vez de "mv" (con "mv" falla por permisos, porque Maven corre como root)
#
# Esta copia vive en el repo para que todos tengan la misma versión.
# En la VM hay que copiarla a ~/infra/deploy.sh y darle permisos: chmod +x ~/infra/deploy.sh
# =============================================================================
set -e

INFRA="$HOME/infra"
REPO="https://github.com/TiziTur/ingweb3.git"
RAMA="main"
CARPETA_TMP="/tmp/ingweb3"

# En la VM el perfil mysqlprod lo define la infraestructura (normalmente catalina.properties).
# Si no está definido, la aplicación arrancaría con el perfil mysqldev y no encontraría la base.
if ! grep -qs "mysqlprod" "$INFRA/tomcat/conf/catalina.properties" "$INFRA/tomcat/bin/setenv.sh" "$INFRA/docker-compose.yml"; then
	echo "ATENCION: no encuentro el perfil mysqlprod en la infraestructura."
	echo "Agregá la línea spring.profiles.active=mysqlprod al final de $INFRA/tomcat/conf/catalina.properties"
	echo "y volvé a correr el deploy."
	exit 1
fi

echo ">> Clonando $REPO ($RAMA)"
cd /tmp
sudo rm -rf "$CARPETA_TMP"
git clone -b "$RAMA" --depth 1 "$REPO" "$CARPETA_TMP"

echo ">> Compilando el .war"
docker run --rm -v "$HOME/.m2:/root/.m2" -v "$CARPETA_TMP/iw3:/usr/src/mymaven" -w /usr/src/mymaven \
	maven:3.9.11-amazoncorretto-21-debian mvn clean package -Dmaven.test.skip=true -Dbuild=war

sudo chmod 664 "$CARPETA_TMP/iw3/target/ROOT.war"
sudo chown "$USER" "$CARPETA_TMP/iw3/target/ROOT.war"

echo ">> Reemplazando la aplicación en Tomcat"
cd "$INFRA"
docker compose stop backend
rm -rf "$INFRA/tomcat/webapps/ROOT"
mkdir "$INFRA/tomcat/webapps/ROOT"
cp "$CARPETA_TMP/iw3/target/ROOT.war" "$INFRA/tomcat/webapps/ROOT/ROOT.zip"
cd "$INFRA/tomcat/webapps/ROOT/"
unzip -q ROOT.zip
rm ROOT.zip
cd "$INFRA"
docker compose up -d backend

echo ">> Listo. Para ver el arranque: docker logs -f backend"
