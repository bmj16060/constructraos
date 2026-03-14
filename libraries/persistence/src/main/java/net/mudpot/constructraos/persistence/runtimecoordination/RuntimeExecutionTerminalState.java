package net.mudpot.constructraos.persistence.runtimecoordination;

public enum RuntimeExecutionTerminalState {
    COMPLETED("completed"),
    FAILED("failed");

    private final String persistedValue;

    RuntimeExecutionTerminalState(final String persistedValue) {
        this.persistedValue = persistedValue;
    }

    public String persistedValue() {
        return persistedValue;
    }
}
