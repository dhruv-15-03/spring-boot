package io.github.dhruv1503.bootusage.autoconfigure;

/**
 * Exception thrown when usage policy violations are detected and fail-on-violation is enabled.
 * <p>
 * This exception is thrown during application startup by the {@link UsagePolicyEnforcer}
 * when one or more {@link UsagePolicy} implementations return violations and the
 * {@code spring.boot.usage.report.policies-fail-on-violation} property is set to {@code true}.
 * <p>
 * The exception message contains details about all violations that were detected,
 * including the policy name and violation message for each.
 *
 * @author Dhruv Bansal
 * @since 1.0.0
 * @see UsagePolicy
 * @see UsagePolicyEnforcer
 */
public class UsagePolicyViolationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int violationCount;

	/**
	 * Creates a new policy violation exception with the specified message.
	 * @param message the detail message describing the violations
	 */
	public UsagePolicyViolationException(String message) {
		super(message);
		this.violationCount = 1;
	}

	/**
	 * Creates a new policy violation exception with the specified message and count.
	 * @param message the detail message describing the violations
	 * @param violationCount the number of violations detected
	 */
	public UsagePolicyViolationException(String message, int violationCount) {
		super(message);
		this.violationCount = violationCount;
	}

	/**
	 * Creates a new policy violation exception with the specified message and cause.
	 * @param message the detail message describing the violations
	 * @param cause the underlying cause
	 */
	public UsagePolicyViolationException(String message, Throwable cause) {
		super(message, cause);
		this.violationCount = 1;
	}

	/**
	 * Get the number of violations that were detected.
	 * @return the violation count
	 */
	public int getViolationCount() {
		return this.violationCount;
	}

}
