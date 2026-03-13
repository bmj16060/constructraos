package net.mudpot.constructraos.codexbridge.codex;

import java.net.URI;
import java.time.Duration;

interface CodexAppServerSessionFactory {
    CodexAppServerSession open(URI uri, Duration timeout);
}
