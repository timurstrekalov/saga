package com.github.timurstrekalov.saga.core;

import java.util.Locale;

public enum ReportFormat {
    HTML,
    RAW {
        @Override
        public String getSuffix() {
            return "coverage";
        }

        @Override
        public String getExtension() {
            return "dat";
        }
    },
    CSV,
    PDF, 
    COBERTURA {
    	@Override
    	public String getSuffix() {
    		return "coverage";
    	}

    	@Override
    	public String getExtension() {
    		return "xml";
    	}
    };

    public String getSuffix() {
        return "report";
    }

    public String getExtension() {
        return name().toLowerCase(Locale.ENGLISH);
    }

}
