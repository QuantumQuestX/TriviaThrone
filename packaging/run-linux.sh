#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -f users.xml ]]; then
    cat > users.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE users SYSTEM "users.dtd">
<users xsi:noNamespaceSchemaLocation="users.xsd"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <user>
        <username>admin</username>
        <displayName>Administrator</displayName>
        <roles/>
    </user>
</users>
EOF
fi

user_count="$(grep -c '<username>' users.xml || true)"
password_hash_count="$(grep -c '<passwordHash>' users.xml || true)"
if (( password_hash_count < user_count )) && [[ -z "${AUTH_INITIAL_PASSWORD:-}" ]]; then
    read -r -s -p "Initial user password: " AUTH_INITIAL_PASSWORD
    printf '\n'
    export AUTH_INITIAL_PASSWORD
fi

if (( password_hash_count < user_count )) && [[ -z "${AUTH_INITIAL_PASSWORD:-}" ]]; then
    echo "AUTH_INITIAL_PASSWORD must be set for users without a password hash." >&2
    exit 1
fi

exec ./bin/TriviaThrone "$@"
