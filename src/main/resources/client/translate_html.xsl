<xsl:stylesheet
        xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='3.0'
        xmlns:translation="http://apache.org/translation"
        xmlns:root="http://apache.org/root"
        xmlns:xfn="http://www.w3.org/2005/xpath-functions"
        exclude-result-prefixes="translation"
>

    <xsl:variable name="json-text" select="//translation:element"/>
    <xsl:variable name="max-count" select="//translation:element/@maxcount"/>
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
                        <!--<tr>-->
                        <!--<td>-->
                        <!--<xsl:value-of select="@key"/>-->
                        <!--</td>-->
                        <!--</tr>-->
                        <xsl:for-each select="xfn:map[@key]">
                            <xsl:sort select="@key"/>
                            <xsl:if test="@key != ''">
                                <tr bgcolor="#F5F5F5" style="font-style: italic;color:gray">
                                    <td align="right">
                                        <xsl:value-of select="@key"/>
                                    </td>
                                </tr>
                            </xsl:if>
                            <!--<xsl:for-each select="xfn:number[@key][not(position() &gt; $max-count)]">-->
                            <xsl:for-each select="xfn:number[@key]">
                                <xsl:sort select="text()" data-type="number" order="descending"/>
                                <xsl:if test="position() &lt;= $max-count">
                                <tr>
                                    <xsl:attribute name="style">
                                        <xsl:choose>
                                            <xsl:when test="position() mod 2 = 0">
                                                background:#EFF9FF
                                            </xsl:when>
                                            <xsl:otherwise>
                                                background:white
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:attribute>
                                    <td>
                                        <span style="white-space:nowrap;"><span style="color:gray;"><xsl:value-of select = "position()" />. </span>
                                        <xsl:value-of select="@key"/><span style="font-style: italic;color:gray;">-<xsl:value-of select="text()"/></span></span>
                                    </td>
                                </tr>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:for-each>
                    </xsl:for-each>
                </table>
            </xsl:when>
            <xsl:otherwise>
                <div>
                    <img src="file:///client/images/loading.gif"/>
                </div>
            </xsl:otherwise>
        </xsl:choose>

        <xsl:variable name="transcription_am">
            <xsl:for-each
                    select='$document-json/xfn:map//xfn:map[@key="transcriptions"]/xfn:array[@key="AM"]/xfn:string'>
                <xsl:sort select='text()'/>
                <xsl:if test="position()=1">
                    <xsl:value-of select="text()"/>
                </xsl:if>
            </xsl:for-each>
        </xsl:variable>

        <xsl:variable name="transcription_br">
            <xsl:for-each
                    select='$document-json/xfn:map//xfn:map[@key="transcriptions"]/xfn:array[@key="BR"]/xfn:string'>
                <xsl:sort select='text()'/>
                <xsl:if test="position()=1">
                    <xsl:value-of select="text()"/>
                </xsl:if>
            </xsl:for-each>
        </xsl:variable>

        <xsl:if test="$transcription_am != '' or $transcription_br != ''">
            <table bgcolor="#EEF5FF" cellpadding="0" cellspacing="0" width="{$width-px}"
                   style="font-family: 'Times New Roman', Times, serif;font-size:small">
                <tr>
                    <td style="color:#8f0610;" align="left">
                        <xsl:if test="$transcription_am">
                            <xsl:value-of select='$transcription_am'/>
                        </xsl:if>
                    </td>
                    <td style="color:#07255e;" align="right">
                        <xsl:if test="$transcription_br">
                            <xsl:value-of select='$transcription_br'/>
                        </xsl:if>
                    </td>
                </tr>
            </table>
        </xsl:if>
    </xsl:template>


    <!--<xsl:template-->
    <!--match="@*|*|text()|processing-instruction()">-->
    <!--<xsl:copy>-->
    <!--<xsl:apply-templates-->
    <!--select="@*|*|text()|processing-instruction()"/>-->
    <!--</xsl:copy>-->
    <!--</xsl:template>-->
</xsl:stylesheet>