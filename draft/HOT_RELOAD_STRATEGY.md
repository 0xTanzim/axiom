# Hot Reload Strategy — Axiom Framework

## Goal
Enable automatic application reload when source files change during development.

## Approach
Use **existing, battle-tested solutions** — NOT building from scratch.

---

## Option 1: Spring Boot DevTools Pattern (Recommended)

### Strategy
- Separate classloader for application code vs framework code
- Watch for file changes in `target/classes`
- Trigger fast restart (not JVM restart)

### Implementation Plan

#### 1. Create `axiom-devtools` Module

```
axiom-devtools/
├── src/main/java/io/axiom/devtools/
│   ├── FileWatcher.java          // Monitors target/classes
│   ├── RestartClassLoader.java   // Custom classloader
│   ├── DevToolsServer.java       // Coordinates reload
│   └── ChangeDetector.java       // Detects class changes
└── pom.xml
```

#### 2. File Watching Mechanism

Use **Java WatchService** (built-in, no dependencies):

```java
// FileWatcher.java
public class FileWatcher {
    private final WatchService watchService;
    private final Path targetDir;

    public FileWatcher(Path targetDir) {
        this.targetDir = targetDir;
        this.watchService = FileSystems.getDefault().newWatchService();
        targetDir.register(watchService,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE
        );
    }

    public void watch(Consumer<Path> onChange) {
        // Watch loop - detects .class file changes
    }
}
```

#### 3. Restart Mechanism

```java
// DevToolsServer.java
public class DevToolsServer {
    private volatile App currentApp;
    private RestartClassLoader appClassLoader;

    public void enableHotReload() {
        FileWatcher watcher = new FileWatcher(Path.of("target/classes"));
        watcher.watch(changedFile -> {
            if (changedFile.toString().endsWith(".class")) {
                restartApp();
            }
        });
    }

    private void restartApp() {
        // 1. Stop current app
        currentApp.stop();

        // 2. Create new classloader
        appClassLoader = new RestartClassLoader(/* app classpath */);

        // 3. Reload main class and restart
        Class<?> mainClass = appClassLoader.loadClass("com.user.Main");
        Method main = mainClass.getMethod("main", String[].class);
        main.invoke(null, (Object) new String[0]);
    }
}
```

#### 4. User Activation

Add to `pom.xml`:

```xml
<dependency>
    <groupId>io.github.0xtanzim</groupId>
    <artifactId>axiom-devtools</artifactId>
    <scope>provided</scope> <!-- dev-time only -->
</dependency>
```

Enable in code:

```java
public static void main(String[] args) {
    if (DevTools.isEnabled()) {  // Checks system property
        DevTools.start();
    }

    App app = Axiom.create();
    // ... routes
    app.listen(8080);
}
```

Or automatic via system property:
```bash
mvn compile exec:java -Daxiom.devtools.enabled=true
```

---

## Option 2: JVM HotSwap (Limited)

### Strategy
Use **JDWP (Java Debug Wire Protocol)** for class reloading.

### Limitations
- Only method body changes (no structural changes)
- No new fields/methods
- Built into Java, no dependencies

### Implementation

```java
// In axiom-devtools
public class JvmHotSwap {
    public static void enableHotSwap() {
        System.setProperty("java.compiler", "NONE");
        // Connect to JVM via JDWP
    }
}
```

**Not recommended** — too limited for real development.

---

## Option 3: Use Existing Tool — JRebel Alternative

### Recommended: **DCEVM + HotSwapAgent**

**Free, open-source, production-ready.**

#### Setup for Users

1. Download DCEVM (enhanced JVM with better hot-reload)
2. Install HotSwapAgent

```bash
# Download
wget https://github.com/TravaOpenJDK/trava-jdk-11-dcevm/releases/download/dcevm-11.0.17%2B1/java11-openjdk-dcevm-linux.tar.gz

# Use with Axiom
JAVA_HOME=/path/to/dcevm java -javaagent:hotswap-agent.jar -jar app.jar
```

#### Integration with Axiom

Create helper script:

```bash
#!/bin/bash
# scripts/dev-mode.sh

echo "[Axiom DevTools] Starting with hot reload..."

# Check if DCEVM available
if [ -z "$DCEVM_HOME" ]; then
    echo "DCEVM not found. Install from: https://dcevm.github.io/"
    echo "Falling back to standard JVM (limited hot-reload)"
    JAVA_CMD="java"
else
    JAVA_CMD="$DCEVM_HOME/bin/java"
fi

# Start with HotSwapAgent
$JAVA_CMD \
    -XX:+AllowEnhancedClassRedefinition \
    -XX:HotswapAgent=fatjar \
    -javaagent:hotswap-agent.jar \
    -jar target/my-app.jar
```

---

## Recommended Solution

**Hybrid Approach:**

1. **Development (Local):**
   - Provide `axiom-devtools` with file watching + classloader restart
   - Fast, simple, zero external dependencies
   - Good enough for most use cases

2. **Power Users:**
   - Document DCEVM + HotSwapAgent setup
   - True hot-swap without restart
   - No framework changes needed

---

## Implementation Phases

### Phase 1: Basic File Watching (Week 1)
- [ ] Create `axiom-devtools` module
- [ ] Implement `FileWatcher` with Java WatchService
- [ ] Implement simple app restart on class change
- [ ] Document activation

### Phase 2: Smart Restart (Week 2)
- [ ] Create separate classloader for app vs framework
- [ ] Preserve server port during restart
- [ ] Add debouncing (batch multiple changes)
- [ ] Add ignore patterns (exclude test files)

### Phase 3: Integration & Polish (Week 3)
- [ ] Maven plugin for auto-enable in dev mode
- [ ] IDE integration documentation (IntelliJ/VSCode)
- [ ] Performance optimization (reload < 500ms)
- [ ] Document DCEVM alternative

---

## API Design (Draft)

### Automatic Activation

```java
// axiom-devtools auto-detects dev mode
public static void main(String[] args) {
    App app = Axiom.create();
    app.use(/* middleware */);
    app.route(/* routes */);
    app.listen(8080);

    // If axiom-devtools is on classpath + dev mode enabled:
    // → Automatically watches and restarts
}
```

### Manual Control

```java
import io.axiom.devtools.DevTools;

public static void main(String[] args) {
    DevTools.enable()
        .watchPath("target/classes")
        .watchPath("src/main/resources")
        .debounce(Duration.ofMillis(300))
        .onReload(() -> System.out.println("🔄 Reloading..."));

    App app = Axiom.create();
    // ...
}
```

---

## Comparison with Alternatives

| Solution | Speed | Structural Changes | Complexity | Dependencies |
|----------|-------|-------------------|------------|--------------|
| **axiom-devtools** | ⚡ Fast (< 1s) | ✅ Yes | 🟢 Low | Zero |
| DCEVM + HotSwapAgent | ⚡⚡ Instant | ✅ Yes | 🟡 Medium | External JVM |
| JVM HotSwap | ⚡⚡ Instant | ❌ No | 🟢 Low | Built-in |
| JRebel | ⚡⚡ Instant | ✅ Yes | 🟢 Low | Commercial ($) |

---

## Next Steps

1. **Create RFC for axiom-devtools design**
2. **Prototype file watcher + classloader restart**
3. **Benchmark reload time vs Spring Boot DevTools**
4. **Document setup for users**

**Status:** Ready to implement Phase 1.
