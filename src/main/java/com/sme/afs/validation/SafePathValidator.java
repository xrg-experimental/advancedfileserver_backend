package com.sme.afs.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Validator that ensures a provided file path is relative and safe, preventing path traversal.
 * <p>
 * Rules:
 * - Normalize separators to '/'
 * - Resolve '.' and '..' segments
 * - Reject absolute paths (leading '/', UNC paths, or Windows drive letters like 'C:')
 * - Reject when normalization would escape the root (too many '..')
 */
public class SafePathValidator implements ConstraintValidator<SafePath, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            // Let @NotBlank handle null/blank if present
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return true;
        }

        // Quick checks for Windows drive letters and UNC paths
        if (startsWithDriveLetter(trimmed)) {
            return false;
        }
        if (startsWithUnc(trimmed)) {
            return false;
        }

        // Normalize separators (include common Unicode slash lookalikes)
        //noinspection UnnecessaryUnicodeEscape
        String path = trimmed
                .replace('\\', '/')
                .replace('\u2215', '/')  // division slash
                .replace('\u2044', '/'); // fraction slash

        // Reject absolute paths (leading '/')
        if (path.startsWith("/")) {
            return false;
        }

        // Resolve path segments and ensure no escape beyond root
        String[] parts = path.split("/");
        Deque<String> stack = new ArrayDeque<>();
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (stack.isEmpty()) {
                    // would escape the intended root
                    return false;
                }
                stack.pop();
                continue;
            }
            // Normal segment
            stack.push(part);
        }

        // Additional safety: ensure no remaining back-references
        for (String seg : stack) {
            if ("..".equals(seg)) {
                return false;
            }
        }

        return true;
    }

    private boolean startsWithDriveLetter(String s) {
        // Examples: C:, C:\foo, D:/bar
        return s.length() >= 2 && Character.isLetter(s.charAt(0)) && s.charAt(1) == ':';
    }

    private boolean startsWithUnc(String s) {
        // \\server\share or //server/share
        return s.startsWith("\\\\") || s.startsWith("//");
    }
}
