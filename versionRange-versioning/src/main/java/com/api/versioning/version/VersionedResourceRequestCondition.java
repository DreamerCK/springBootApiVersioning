package com.api.versioning.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.condition.AbstractRequestCondition;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionedResourceRequestCondition extends AbstractRequestCondition<VersionedResourceRequestCondition> {
    private Logger logger = LoggerFactory.getLogger(VersionedResourceRequestCondition.class);
    private final Set<VersionRange> versions;

    public VersionedResourceRequestCondition(String from, String to) {
        this(versionRange(from, to));
    }

    public VersionedResourceRequestCondition(Collection<VersionRange> versions) {
        this.versions = Collections.unmodifiableSet(new HashSet<VersionRange>(versions));
    }

    private static Set<VersionRange> versionRange(String from, String to) {
        HashSet<VersionRange> versionRanges = new HashSet<>();

        if (StringUtils.hasText(from)) {
            String toVersion = (StringUtils.hasText(to) ? to : Version.MAX_VERSION);
            VersionRange versionRange = new VersionRange(from, toVersion);

            versionRanges.add(versionRange);
        }

        return versionRanges;
    }

    @Override
    public VersionedResourceRequestCondition combine(VersionedResourceRequestCondition other) {
        logger.debug("Combining:\n{}\n{}", this, other);
        Set<VersionRange> newVersions = new LinkedHashSet<VersionRange>(this.versions);
        newVersions.addAll(other.versions);
        return new VersionedResourceRequestCondition(newVersions);
    }

    @Override
    public VersionedResourceRequestCondition getMatchingCondition(HttpServletRequest request) {

        String accept = request.getHeader("Accept");
        Pattern regexPattern = Pattern.compile("(.*)-(\\d+\\.\\d+).*");
        Matcher matcher = regexPattern.matcher(accept);
        if (matcher.matches()) {
            String actualMediaType = matcher.group(1);
            String version = matcher.group(2);
            logger.debug("Version={}", version);

                for (VersionRange versionRange : versions) {
                    if (versionRange.includes(version)) {
                        return this;
                    }
                }
        }

        logger.debug("Didn't find matching version");
        return null;
    }

    @Override
    public int compareTo(VersionedResourceRequestCondition other, HttpServletRequest request) {
        return 0;
    }

    @Override
    protected Collection<?> getContent() {
        return versions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("version={");
        for (VersionRange range : versions) {
            sb.append(range).append(",");
        }
        sb.append("}");

        return sb.toString();
    }

    @Override
    protected String getToStringInfix() {
        return " && ";
    }
}
