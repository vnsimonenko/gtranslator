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
    <xsl:variable name="document-json" select="json-to-xml($json-text, map{'unescape': false()})"/>

    <xsl:output encoding="UTF-8" method="text"/>

    <xsl:template match="root:document">
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

        <xsl:value-of select="$transcription_am"/> : <xsl:value-of select="$transcription_br"/>
    </xsl:template>


    <!--<xsl:template-->
    <!--match="@*|*|text()|processing-instruction()">-->
    <!--<xsl:copy>-->
    <!--<xsl:apply-templates-->
    <!--select="@*|*|text()|processing-instruction()"/>-->
    <!--</xsl:copy>-->
    <!--</xsl:template>-->
</xsl:stylesheet>