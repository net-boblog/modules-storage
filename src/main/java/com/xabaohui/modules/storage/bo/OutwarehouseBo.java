package com.xabaohui.modules.storage.bo;

import java.util.List;

import com.xabaohui.modules.storage.dto.DistrBatchOutOfStockRequestDTO;
import com.xabaohui.modules.storage.dto.DistrBatchOutOfStockResponseDTO;
import com.xabaohui.modules.storage.dto.DistrBatchOutOfStockResponseDetail;
import com.xabaohui.modules.storage.entiry.StorageIoDetail;
import com.xabaohui.modules.storage.entiry.StorageIoTask;

public interface OutwarehouseBo {

	/**
	 * 添加一次出库操作<列表>
	 * @param storageIoTask
	 * @param listResponseDetails(出库单明细细)
	 */
	void addOutwarehouseOperate(StorageIoTask task,List<StorageIoDetail> listStorageIoDetails);
	
	/**
	 * 执行出库操作<根据skuId+amount>
	 * @param skuId
	 * @param amount
	 * @return List<StorageIoDetail>出库明细单
	 */
	int addOutwarehouseOperate(StorageIoTask task, Integer skuId,Integer amount);
	 
	/**
	 *  执行出库操作<根据skuId+amount+positionId>
	 * @param taskId/checkId
	 * @param skuId
	 * @param amount
	 * @param positionId
	 * @param ioDetailType
	 */
	void addOutwarehouseOperate(Integer taskId,Integer skuId,Integer amount,Integer positionId,String ioDetailType);
	
	/**
	 *  查询出库明细并封装成配货明细
	 * @param taskId
	 * @param lisIoDetails(null||List<StorageIoDetail>)
	 * @return
	 */
	List<DistrBatchOutOfStockResponseDetail> findResponseListDetail(Integer taskId);
	
	/**
	 * 获取请求结果
	 * @param failReasons
	 * @param taskId
	 * @return
	 */
	DistrBatchOutOfStockResponseDTO buildResponseDto(String failReasons,Integer taskId);
	
	/**
	 * 出错重配：添加变动明细缺配标记
	 * @param dbsr
	 */
	void processAddErrorInfo(DistrBatchOutOfStockRequestDTO dbsr);
	
	/**
	 * 出错重配:参数检查
	 * @param dbsr
	 * @return taskId
	 */
	void processCheckParamter(DistrBatchOutOfStockRequestDTO dbsr);

	/**
	 * 更新库存:占用
	 * @param skuId
	 * @param amount
	 */
	void updateStorageProductOccupyStock(Integer skuId, Integer amount);
	
	/**
	 * 更新库存:取消占用
	 * @param skuId
	 * @param amount
	 */
	void updateStorageProductRemoveOccupyStorage(Integer skuId, Integer amount);
	
}
