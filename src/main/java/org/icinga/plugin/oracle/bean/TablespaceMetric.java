package org.icinga.plugin.oracle.bean;

public class TablespaceMetric {

	private String tablespaceName;
	private Float usedCapacityPct;
	private Float freeCapacityPct;
	private Double totalCapacityMb;
	private Double usedCapacityMb;
	private Double freeCapacityMb;

	/**
	 * @param tablespaceName
	 * @param usedCapacityPct
	 * @param freeCapacityPct
	 * @param totalCapacityMb
	 * @param usedCapacityMb
	 * @param freeCapacityMb
	 */
	public TablespaceMetric(String tablespaceName, Float usedCapacityPct, Float freeCapacityPct, Double totalCapacityMb,
			Double usedCapacityMb, Double freeCapacityMb) {
		this.tablespaceName = tablespaceName;
		this.usedCapacityPct = usedCapacityPct;
		this.freeCapacityPct = freeCapacityPct;
		this.totalCapacityMb = totalCapacityMb;
		this.usedCapacityMb = usedCapacityMb;
		this.freeCapacityMb = freeCapacityMb;
	}

	public TablespaceMetric() {
	}

	public final String getTablespaceName() {
		return tablespaceName;
	}

	public final void setTablespaceName(String tablespaceName) {
		this.tablespaceName = tablespaceName;
	}

	public final Float getUsedCapacityPct() {
		return usedCapacityPct;
	}

	public final void setUsedCapacityPct(Float usedCapacityPct) {
		this.usedCapacityPct = usedCapacityPct;
	}

	public final Float getFreeCapacityPct() {
		return freeCapacityPct;
	}

	public final void setFreeCapacityPct(Float freeCapacityPct) {
		this.freeCapacityPct = freeCapacityPct;
	}

	public final Double getTotalCapacityMb() {
		return totalCapacityMb;
	}

	public final void setTotalCapacityMb(Double totalCapacityMb) {
		this.totalCapacityMb = totalCapacityMb;
	}

	public final Double getUsedCapacityMb() {
		return usedCapacityMb;
	}

	public final void setUsedCapacityMb(Double usedCapacityMb) {
		this.usedCapacityMb = usedCapacityMb;
	}

	public final Double getFreeCapacityMb() {
		return freeCapacityMb;
	}

	public final void setFreeCapacityMb(Double freeCapacityMb) {
		this.freeCapacityMb = freeCapacityMb;
	}

}
