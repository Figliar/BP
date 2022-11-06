# Path to this extension
JAR_DIR="Modules/target"
# Path to TikaConfig file
TIKA_CONFIG="Modules/src/main/resources/re/parsers/tika-config-bc.xml"
# Argument = File to parse
FILE="$1"

exec java \
    -jar "${JAR_DIR}/ReFormat.jar" \
    --config="${TIKA_CONFIG}" \
    "${FILE}"
