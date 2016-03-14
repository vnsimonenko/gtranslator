package com.saxonica.config;

import net.sf.saxon.Configuration;
import net.sf.saxon.trans.LicenseException;

public class ProfessionalConfiguration extends Configuration {
    protected void needEnterpriseEdition() {
    }
    public void checkLicensedFeature(int feature, String name, int localLicenseId) throws LicenseException {
    }

    public void setConfigurationProperty(String name, Object value) {
        try {
            super.setConfigurationProperty(name, value);
            this.getDefaultXsltCompilerInfo().setXsltVersion(30);
        } catch (Exception ex) {
            //ex.printStackTrace();
        }
    }
}
