package com.xabaohui.modules.storage.service;

import java.util.List;

import com.xabaohui.modules.storage.dto.DistrBatchOutOfStockRequestDTO;
import com.xabaohui.modules.storage.dto.DistrBatchOutOfStockResponseDTO;
import com.xabaohui.modules.storage.dto.DistrBatchRequestDTO;
import com.xabaohui.modules.storage.dto.DistrBatchRequestDetail;
import com.xabaohui.modules.storage.dto.StorageCheckDiffReaultDetail;
import com.xabaohui.modules.storage.dto.StorageCheckSnapDataDetail;
import com.xabaohui.modules.storage.dto.StorageInwarehouseDataDetail;
import com.xabaohui.modules.storage.entiry.StorageCheck;
import com.xabaohui.modules.storage.entiry.StorageCheckPlan;
import com.xabaohui.modules.storage.entiry.StorageIoDetail;
import com.xabaohui.modules.storage.entiry.StorageIoTask;

/**
 * 配货接口
 * 
 * @author cxin
 * 
 */
public interface StorageOperaterService {
	
	/**
	 * 首次配货，返回流水
	 * @param distrBatchRequestDTO
	 * @return
	 */
	DistrBatchOutOfStockResponseDTO firstDistriApply(
			DistrBatchRequestDTO distrBatchRequestDTO);

	/**
	 * 重新打印配货单
	 * @param taskId
	 * @return
	 */
	DistrBatchOutOfStockResponseDTO againDistriApply(Integer taskId);

	/**
	 * 出错补配
	 * @param distrBatchOutOfStockRequestDTO
	 * @return
	 */
	DistrBatchOutOfStockResponseDTO errorOutDetailFill(
			DistrBatchOutOfStockRequestDTO distrBatchOutOfStockRequestDTO);

	/**
	 * 占用库存
	 * @param list
	 */
	void occupyStorageProduct(List<DistrBatchRequestDetail> list);

	/**
	 * 取消占用
	 * @param list
	 */
	void removeOccupyStorageProduct(List<DistrBatchRequestDetail> list);

	/**
	 * 创建库存变动单
	 * @param operator
	 * @param memo
	 * @param operateType
	 * @return
	 */
	StorageIoTask createStorageIoTask(Integer operator,String memo,String operateType);
	
	/**
	 * 列表入库
	 * @param operator
	 * @param memo
	 * @param operateType
	 * @param listDetails
	 * @return
	 */
	List<StorageIoDetail> storageInWareHouse(Integer operator,String memo,String operateType,List<StorageInwarehouseDataDetail> listDetails);
	
	/**
	 *  单条入库
	 * @param taskId
	 * @param skuId
	 * @param amount
	 * @param positionId
	 */
	void storageInWareHouse(Integer taskId, Integer skuId, Integer amount, Integer positionId);
	
	/**
	 * 创建盘点计划
	 * @param operator
	 * @param amount
	 * @param memo
	 * @return
	 */
	StorageCheckPlan createStorageCheckPlan(Integer operator, String memo);

	/**
	 * 创建盘点
	 * @param planId
	 * @param operator
	 * @param positionId
	 * @param memo
	 * @return
	 */
	StorageCheck createStorageCheck(Integer planId, Integer operator,
			Integer positionId, String memo);

	/**
	 * 一盘<插入数据并比较出结果>
	 * @param list
	 * @param checkId
	 * @return 
	 */
	List<StorageCheckDiffReaultDetail> firstStorageCheck(
			List<StorageCheckSnapDataDetail> list, Integer checkId);

	/**
	 * 二盘<插入数据并比较出结果>
	 * @param list
	 * @param checkId
	 * @return null/List<StorageCheckDiffReaultDetail>
	 */
	List<StorageCheckDiffReaultDetail> secondStorageCheck(
			List<StorageCheckSnapDataDetail> list, Integer checkId);

	/**
	 * 盘点调整<单个调整>
	 * @param operator
	 * @param memo
	 * @param checkId
	 * @param skuId
	 * @param realAmount<指定库位库存明细数量>
	 * @param storageIoTask
	 */
	void adjustMentCheckResult(Integer operator, String memo, Integer checkId, Integer skuId, Integer realAmount);
	
	/**
	 * 解锁库位<所有调整完成之后>
	 * @param positionId
	 */
	void  unLockPosition(Integer operator,Integer positionId);
	
}
