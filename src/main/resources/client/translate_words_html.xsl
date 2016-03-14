<xsl:stylesheet
        xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='3.0'
        xmlns:translation="http://apache.org/translation"
        xmlns:root="http://apache.org/root"
        xmlns:xfn="http://www.w3.org/2005/xpath-functions"
        exclude-result-prefixes="translation"
>

    <xsl:variable name="jsonfile"
                  select="unparsed-text('file:///home/vns/workspace/desktop/gtranslator/trax/xml/test.json')"/>
    <xsl:variable name="json-text" select="//translation:element"/>
    <xsl:variable name="width-px" select="//translation:element/@width"/>
    <xsl:variable name="height-px" select="//translation:element/@height"/>
    <xsl:variable name="font-family" select="//translation:element/@fontfamily"/>
    <xsl:variable name="document-json" select="json-to-xml($json-text, map{'unescape': false()})"/>

    <xsl:output encoding="UTF-8" method="html"/>

    <xsl:template match="root:document">
        <!--<xsl:value-of select="$document-json/xfn:map/xfn:string[@key='source']"/>-->

        <xsl:choose>
            <xsl:when test="$document-json/xfn:map//xfn:map[@key='translations'] != ''">
                <table cellpadding="0" cellspacing="0"
                       style="font-family: '{$font-family}';font-size:small" width="{$width-px}" height="{$height-px}">
                    <xsl:for-each select="$document-json/xfn:map//xfn:map[@key='translations']/xfn:map[@key]">
                        <xsl:sort select="@key"/>
                        <xsl:for-each select="xfn:map[@key]">
                            <xsl:sort select="@key"/>
                            <xsl:if test="@key != ''">
                                <tr bgcolor="#F5F5F5" style="font-style: italic;color:gray">
                                    <td align="right">
                                        <xsl:value-of select="@key"/>
                                    </td>
                                </tr>
                            </xsl:if>
                            <xsl:for-each select="xfn:number[@key]">
                                <xsl:sort select="@key"/>
                                <tr>
                                    <td>
                                        <xsl:value-of select="@key"/>
                                    </td>
                                </tr>
                            </xsl:for-each>
                        </xsl:for-each>
                    </xsl:for-each>
                </table>
            </xsl:when>
            <xsl:otherwise>
                <div>
                    <image src="file:///client/images/loading.gif"/>
                </div>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!--<xsl:template-->
    <!--match="@*|*|text()|processing-instruction()">-->
    <!--<xsl:copy>-->
    <!--<xsl:apply-templates-->
    <!--select="@*|*|text()|processing-instruction()"/>-->
    <!--</xsl:copy>-->
    <!--</xsl:template>-->
</xsl:stylesheet>