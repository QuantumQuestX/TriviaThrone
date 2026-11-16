package quantumquest.triviathrone.api.shared;

/**
 * Numeric major, minor, and patch components of a semantic version.
 *
 * @param major major version component
 * @param minor minor version component
 * @param patch patch version component
 * @author TiJ
 * @since 2.1.0
 */
public record Version(int major, int minor, int patch) implements Comparable<Version> {
    /**
     * Creates a version and rejects negative components.
     *
     * @param major major version component
     * @param minor minor version component
     * @param patch patch version component
     * @throws IllegalArgumentException if a component is negative
     */
    public Version {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("version segment cannot be negative");
        }
    }

    /**
     * Compares versions by major, then minor, then patch component.
     *
     * @param other version to compare with
     * @return comparison result
     */
    @Override
    public int compareTo(Version other) {
        int result = Integer.compare(major, other.major);

        if (result == 0) {
            result = Integer.compare(minor, other.minor);
        }

        if (result == 0) {
            result = Integer.compare(patch, other.patch);
        }

        return result;
    }
}
