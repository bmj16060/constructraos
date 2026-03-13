package net.mudpot.constructraos.apiservice.mcp;

import io.micronaut.mcp.annotations.Tool;
import jakarta.inject.Singleton;

@Singleton
public class HelloMcpTool {
    @Tool(
        name = "hello",
        description = "Return a basic greeting from the ConstructraOS API.",
        annotations = @Tool.ToolAnnotations(
            title = "Hello",
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false,
            returnDirect = true
        )
    )
    public String hello() {
        return "Hello from ConstructraOS API.";
    }
}
