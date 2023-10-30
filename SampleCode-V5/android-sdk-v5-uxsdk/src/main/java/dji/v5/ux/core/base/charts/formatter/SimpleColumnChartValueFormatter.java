package dji.v5.ux.core.base.charts.formatter;

import dji.v5.ux.core.base.charts.model.SubcolumnValue;

public class SimpleColumnChartValueFormatter implements ColumnChartValueFormatter {
    private ValueFormatterHelper valueFormatterHelper;

    public SimpleColumnChartValueFormatter() {
        this.valueFormatterHelper = new ValueFormatterHelper();
        this.valueFormatterHelper.determineDecimalSeparator();
    }

    public SimpleColumnChartValueFormatter(int decimalDigitsNumber) {
        this();
        this.valueFormatterHelper.setDecimalDigitsNumber(decimalDigitsNumber);
    }

    public int formatChartValue(char[] formattedValue, SubcolumnValue value) {
        return this.valueFormatterHelper.formatFloatValueWithPrependedAndAppendedText(formattedValue, value.getValue(), value.getLabelAsChars());
    }

    public int getDecimalDigitsNumber() {
        return this.valueFormatterHelper.getDecimalDigitsNumber();
    }

    public SimpleColumnChartValueFormatter setDecimalDigitsNumber(int decimalDigitsNumber) {
        this.valueFormatterHelper.setDecimalDigitsNumber(decimalDigitsNumber);
        return this;
    }

    public char[] getAppendedText() {
        return this.valueFormatterHelper.getAppendedText();
    }

    public SimpleColumnChartValueFormatter setAppendedText(char[] appendedText) {
        this.valueFormatterHelper.setAppendedText(appendedText);
        return this;
    }

    public char[] getPrependedText() {
        return this.valueFormatterHelper.getPrependedText();
    }

    public SimpleColumnChartValueFormatter setPrependedText(char[] prependedText) {
        this.valueFormatterHelper.setPrependedText(prependedText);
        return this;
    }

    public char getDecimalSeparator() {
        return this.valueFormatterHelper.getDecimalSeparator();
    }

    public SimpleColumnChartValueFormatter setDecimalSeparator(char decimalSeparator) {
        this.valueFormatterHelper.setDecimalSeparator(decimalSeparator);
        return this;
    }
}

