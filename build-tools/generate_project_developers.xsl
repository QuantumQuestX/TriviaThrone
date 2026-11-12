<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:m="http://maven.apache.org/POM/4.0.0"
                exclude-result-prefixes="m">
    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/">
        <developers>
            <xsl:for-each select="/m:project/m:developers/m:developer[normalize-space(m:name) or normalize-space(m:id)]">
                <developer>
                    <name>
                        <xsl:choose>
                            <xsl:when test="normalize-space(m:name)">
                                <xsl:value-of select="normalize-space(m:name)"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="normalize-space(m:id)"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </name>
                    <profileUrl>
                        <xsl:value-of select="normalize-space(m:url)"/>
                    </profileUrl>
                    <roles>
                        <xsl:for-each select="m:roles/m:role[normalize-space(.)]">
                            <role>
                                <xsl:value-of select="normalize-space(.)"/>
                            </role>
                        </xsl:for-each>
                    </roles>
                    <organizations>
                        <xsl:variable name="organization-count" select="count(m:organization)"/>
                        <xsl:variable name="url-count" select="count(m:organizationUrl)"/>
                        <xsl:variable name="total">
                            <xsl:choose>
                                <xsl:when test="$organization-count &gt; $url-count">
                                    <xsl:value-of select="$organization-count"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="$url-count"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:call-template name="organizations">
                            <xsl:with-param name="index" select="1"/>
                            <xsl:with-param name="total" select="number($total)"/>
                        </xsl:call-template>
                    </organizations>
                </developer>
            </xsl:for-each>
        </developers>
    </xsl:template>

    <xsl:template name="organizations">
        <xsl:param name="index"/>
        <xsl:param name="total"/>
        <xsl:if test="$index &lt;= $total">
            <organization>
                <name>
                    <xsl:choose>
                        <xsl:when test="normalize-space(m:organization[$index])">
                            <xsl:value-of select="normalize-space(m:organization[$index])"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="normalize-space(m:organizationUrl[$index])"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </name>
                <url>
                    <xsl:value-of select="normalize-space(m:organizationUrl[$index])"/>
                </url>
            </organization>
            <xsl:call-template name="organizations">
                <xsl:with-param name="index" select="$index + 1"/>
                <xsl:with-param name="total" select="$total"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>