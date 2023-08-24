package dji.v5.ux.core.base.charts.formatter;

import dji.v5.ux.core.base.charts.model.PointValue;

public class SimpleLineChartValueFormatter implements LineChartValueFormatter {
    private ValueFormatterHelper valueFormatterHelper;

    public SimpleLineChartValueFormatter() {
        this.valueFormatterHelper = new ValueFormatterHelper();
        this.valueFormatterHelper.determineDecimalSeparator();
    }

    public SimpleLineChartValueFormatter(int decimalDigitsNumber) {
        this();
        this.valueFormatterHelper.setDecimalDigitsNumber(decimalDigitsNumber);
    }

    public int formatChartValue(char[] formattedValue, PointValue value) {
        return this.valueFormatterHelper.formatFloatValueWithPrependedAndAppendedText(formattedValue, value.getY(), value.getLabelAsChars());
    }

    public int getDecimalDigitsNumber() {
        return this.valueFormatterHelper.getDecimalDigitsNumber();
    }

    public SimpleLineChartValueFormatter setDecimalDigitsNumber(int decimalDigitsNumber) {
        this.valueFormatterHelper.setDecimalDigitsNumber(decimalDigitsNumber);
        return this;
    }

    public char[] getAppendedText() {
        return this.valueFormatterHelper.getAppendedText();
    }

    public SimpleLineChartValueFormatter setAppendedText(char[] appendedText) {
        this.valueFormatterHelper.setAppendedText(appendedText);
        return this;
    }

    public char[] getPrependedText() {
        return this.valueFormatterHelper.getPrependedText();
    }

    public SimpleLineChartValueFormatter setPrependedText(char[] prependedText) {
        this.valueFormatterHelper.setPrependedText(prependedText);
        return this;
    }

    public char getDecimalSeparator() {
        return this.valueFormatterHelper.getDecimalSeparator();
    }

    public SimpleLineChartValueFormatter setDecimalSeparator(char decimalSeparator) {
        this.valueFormatterHelper.setDecimalSeparator(decimalSeparator);
        return this;
    }
}

