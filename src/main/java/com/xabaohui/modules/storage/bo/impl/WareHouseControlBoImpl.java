package com.xabaohui.modules.storage.bo.impl;

import java.sql.Timestamp;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;


import com.xabaohui.modules.storage.dao.StorageCheckDao;
import com.xabaohui.modules.storage.dao.StorageCheckDiffAdjustDao;
import com.xabaohui.modules.storage.dao.StorageCheckDiffDao;
import com.xabaohui.modules.storage.dao.StorageCheckPlanDao;
import com.xabaohui.modules.storage.dao.StorageCheckSnapDao;
import com.xabaohui.modules.storage.dao.StorageIoDetailDao;
import com.xabaohui.modules.storage.dao.StorageIoTaskDao;
import com.xabaohui.modules.storage.dao.StoragePosStockDao;
import com.xabaohui.modules.storage.dao.StoragePositionDao;
import com.xabaohui.modules.storage.dao.StorageProductDao;
import com.xabaohui.modules.storage.entiry.StorageCheck;
import com.xabaohui.modules.storage.entiry.StorageCheckDiff;
import com.xabaohui.modules.storage.entiry.StorageCheckDiffAdjust;
import com.xabaohui.modules.storage.entiry.StorageCheckPlan;
import com.xabaohui.modules.storage.entiry.StorageCheckSnap;
import com.xabaohui.modules.storage.entiry.StorageIoDetail;
import com.xabaohui.modules.storage.entiry.StorageIoTask;
import com.xabaohui.modules.storage.entiry.StoragePosStock;
import com.xabaohui.modules.storage.entiry.StoragePosition;
import com.xabaohui.modules.storage.entiry.StorageProduct;


public abstract class WareHouseControlBoImpl{
	@Resource
	protected StoragePositionDao storagePositionDao;
	@Resource
	protected StorageIoTaskDao storageIoTaskDao;
	@Resource
	protected StorageIoDetailDao storageIoDetailDao;
	@Resource
	protected StorageProductDao storageProductDao;
	@Resource
	protected StoragePosStockDao storagePosStockDao;
	@Resource
	protected StorageCheckSnapDao storageCheckSnapDao;
	@Resource
	protected StorageCheckDiffDao storageCheckDiffDao;
	@Resource
	protected StorageCheckPlanDao storageCheckPlanDao;
	@Resource
	protected StorageCheckDiffAdjustDao storageCheckDiffAdjustDao;
	@Resource
	protected StorageCheckDao storageCheckDao;

	protected static Timestamp nowTime(){
		return new Timestamp(System.currentTimeMillis());
	}
	
	protected Logger log = getLogger();
	/**
	 * 生成Logger，由子类负责实现
	 * @return
	 */
	protected abstract Logger getLogger();
			
	/**
	 * 锁定库存
	 * @param operator
	 * @param skuId
	 * @param lockReason锁定原因
	 */
	public void lockStorageProduct(Integer operator, Integer skuId,String lockReason) {
		if (operator < 0 || operator == 0) {
			throw new IllegalArgumentException("输入有误！操作员Id不能为负数和0！！！");
		}
		if (skuId < 0 || skuId == 0) {
			throw new IllegalArgumentException("输入有误！商品Id不能为负数和0！！！");
		}
		if (StringUtils.isBlank(lockReason)) {
			throw new IllegalArgumentException("输入有误！锁定原因不能为空或者空字符串或者空格！！！");
		}
		StorageProduct instance = findStorageProductBySkuId(skuId);
		if (instance.getLockFlag() == true) {
			throw new RuntimeException("库存已锁定！！！锁定原因为:"	+ instance.getLockReason());
		}
		instance.setLockFlag(true);
		instance.setLockReason("operator：" + operator + "：" + lockReason);
		updateStorageProduct(instance);
	}

	/**
	 * 解锁库存
	 * @param operator操作员
	 * @param skuId
	 */
	public void unLockStorageProduct(Integer operator, Integer skuId) {
		if (operator <= 0 || operator == null) {
			throw new IllegalArgumentException("输入有误！操作员Id不能为负数和0！！！");
		}
		if (skuId <= 0 || skuId == null) {
			throw new IllegalArgumentException("输入有误！商品Id不能为负数和0！！！");
		}
		StorageProduct instance = findStorageProductBySkuId(skuId);
		if (instance.getLockFlag() == false) {
			throw new RuntimeException("库存已解锁，无需重复解锁！！！");
		}
		instance.setLockFlag(false);
		instance.setLockReason(null);
		updateStorageProduct(instance);
	}

	/**
	 * 锁定库位：--首先判断是否已经加锁
	 * @param operator
	 * @param positionId
	 * @param lockReason
	 */
	public void lockStoragePosition(Integer operator, Integer positionId,
			String lockReason) {
		if (StringUtils.isBlank(lockReason)) {
			throw new IllegalArgumentException("输入有误！锁定原因不能为空或者空字符串或者空格！！！");
		}
		StoragePosition instance = storagePositionDao.findById(positionId);	// positionId是否存在
		if (instance == null) {
			throw new IllegalArgumentException("输入有误！库位不存在！！！");
		}
		if (instance.getPositionIslock() == true) {// 检查是否已经加锁
			throw new RuntimeException("库位已加锁,不能重复加锁！！！加锁原因："+ instance.getLockReason());
		}
		instance.setPositionIslock(true);
		instance.setLockReason(lockReason);
		updateStoragePosition(instance);
	}

	/**
	 * 解锁库位
	 * @param skuId
	 * @param positionId
	 */
	public void unLockStoragePosition(Integer operator, Integer positionId) {
		if (operator <= 0 || operator == null) {
			throw new IllegalArgumentException("输入有误！操作员Id不能为负数和0！！！");
		}
		if (positionId <= 0 || positionId == null) {
			throw new IllegalArgumentException("输入有误！库位Id不能为负数和0！！！");
		}
		StoragePosition instance = storagePositionDao.findById(positionId);
		if (instance == null) {
			throw new IllegalArgumentException("输入有误！库位不存在！！！");
		}
		if (instance.getPositionIslock() == false) {// 检查是否已经解锁
			throw new RuntimeException("库位已解锁，无需重复解锁！！！");
		}
		instance.setPositionIslock(false);
		updateStoragePosition(instance);
	}

	/**
	 *  更新库位库存明细出错重配标记
	 * @param skuId
	 * @param positionId
	 * @param mark
	 * @param num
	 */
	public void updateStoragePosStockMark(Integer skuId, Integer positionId,
			String mark, Integer num) {
		if (skuId <= 0 || skuId == null) {
			throw new IllegalArgumentException("输入有误！库存商品Id不能为负数和0！！！");
		}
		if (positionId <= 0 || positionId == null) {
			throw new IllegalArgumentException("输入有误！库位Id不能为负数和0！！！");
		}
		if (StringUtils.isBlank(mark)) {
			throw new IllegalArgumentException("输入有误！标记不能为空！！！");
		}
		if (skuId < 0 || skuId == 0) {
			throw new IllegalArgumentException("输入有误！出错数量不能为负数和0！！！");
		}
		StoragePosStock storagePosStock = findStoragePosStockBySkuIdAndPositionId(skuId, positionId);
		if(storagePosStock==null){
			throw new RuntimeException("库位库存明细为空");
		}
		//TODO记录缺货数据，人工处理，没有必要对库位库存做标记
		//storagePosStock.setMark(mark + " 缺货数量：" + num);
		//updateStoragePosStock(storagePosStock);
	}

	protected StorageIoTask processGetIoTask(Integer operator, Integer amount,String memo) {
		StorageIoTask storageIoTask = new StorageIoTask();
		storageIoTask.setUserId(operator);
		storageIoTask.setAmount(amount);
		storageIoTask.setMemo(memo);
		return saveStorageIoTask(storageIoTask);
	}
	
	/**
	 * 添加库存变动单明细(单个操作)
	 * @param positionId库位
	 * @param taskId操作ID
	 * @param skuId商品sku
	 * @param amount数量
	 */
	public void addStorageIoTaskDetail(Integer taskId,Integer positionId,Integer skuId,Integer amount,String ioDetailType) {
		if (skuId <=0 || skuId == null) {
			throw new RuntimeException("skuId不能为空,0或者负数");
		}
		if (amount <=0 || amount == null) {
			throw new RuntimeException("变动数量不能为空,0或者负数");
		}
		if (taskId <=0 || skuId == null) {
			throw new RuntimeException("taskId不能为空,0或者负数");
		}
		if (positionId <=0 || amount == null) {
			throw new RuntimeException("positionId不能为空,0或者负数");
		}
		StorageIoDetail ioDetail=new StorageIoDetail();
		ioDetail.setTaskId(taskId);
		ioDetail.setPositionId(positionId);
		ioDetail.setSkuId(skuId);
		ioDetail.setAmount(amount);
		ioDetail.setIoDetailType(ioDetailType);
		saveStorageIoDetail(ioDetail);
	} 

	/**
	 * 添加库存变动单明细(批量操作) 
	 */
	public void addStorageIoTaskDetail(List<StorageIoDetail> list) {
		if(list==null||list.isEmpty()){
			throw new RuntimeException("变动单明细list为空！！");
		}
		for (StorageIoDetail ioDetail : list) {
			saveStorageIoDetail(ioDetail);
		}
	}
	
	/**
	 * 更新库存:出错返库
	 * @param skuId
	 * @param amount
	 */
//	public void updateStorageProductErrorReturnStock(Integer skuId, Integer amount){//出错返库更新库存
//		checkForPamarater(skuId, amount);
//		StorageProduct storageProduct = findStorageProduct(skuId);
//		storageProduct.setStockAmt(storageProduct.getStockAmt() + amount);// 总库存增加
//		storageProduct.setStockOccupy(storageProduct.getStockOccupy() + amount);// 占用用库存增加
//		updateStorageProduct(storageProduct);
//	}
	
	/**
	 * 更新库存:<库存调整增加>
	 * @param skuId
	 * @param amount
	 */
	public void updateStorageProductChangeAddStorage(Integer skuId, Integer amount){//修改添加库存
		checkForPamarater(skuId,amount);
		StorageProduct storageProduct = findStorageProductBySkuId(skuId);//findStorageProduct(skuId);
		storageProduct.setStockAmt(storageProduct.getStockAmt() + amount);// 总库存增加
		storageProduct.setStockAvailabe(storageProduct.getStockAvailabe() + amount);//可用库存增加
		updateStorageProduct(storageProduct);
	}
	
	/**
	 * 更新库存:<库存调整减少>
	 * @param skuId
	 * @param amount
	 */
	public void updateStorageProductChangeReduceStorage(Integer skuId, Integer amount){//修改减少库存
		checkForPamarater(skuId,amount);
		StorageProduct storageProduct = findStorageProduct(skuId);
		if(storageProduct.getStockAvailabe()<amount){ 
			throw new RuntimeException("减少数量不能小于可用库存量");
		}
		storageProduct.setStockAmt(storageProduct.getStockAmt() -amount);// 总库存减少
		storageProduct.setStockAvailabe(storageProduct.getStockAvailabe() - amount);//可用库存减少
		updateStorageProduct(storageProduct);
	}
	
	protected StorageProduct findStorageProduct(Integer skuId) {
		List<StorageProduct> list = findStorageProductListBySkuId(skuId);
		if (list == null || list.isEmpty()) {
			throw new RuntimeException("skuId输入有误,未找到商品库存");
		}
		if (list.size() > 1) {
			throw new RuntimeException("skuId对应多个库存量！！！");
		}
		return (StorageProduct) list.get(0);
	}

	protected void checkForPamarater(Integer skuId, Integer amount) {//参数检查
		if (skuId == null||skuId <=0) {
			throw new RuntimeException("skuId不能为0或者负数");
		}
		if (amount == null||amount <=0) {
			throw new RuntimeException("变动数量不能为0或者负数");
		}
	}
	
	/**
	 * 更新库位库存明细<入库>
	 * @param instance更新对象
	 * @param amount数量
	 * 
	 */
	public void updateStoragePosStockInwarehouse(Integer skuId,Integer positionId,	Integer amount) {
		pamarterCheck( skuId, positionId, amount);
		StoragePosStock posStock=findStoragePosStockBySkuIdAndPositionId(skuId, positionId);
		if(posStock==null){
			throw new RuntimeException("库位库存明细为空");
		}
		posStock.setAmount(posStock.getAmount() + amount);
		updateStoragePosStock(posStock);
	}
	
	/**
	 * 更新库位库存明细<出库>
	 * @param instance更新对象
	 * @param amount数量
	 * 
	 */
	public void updateStoragePosStockOutwarehouse(Integer skuId, Integer positionId,Integer amount) {
		pamarterCheck(skuId,positionId, amount);
		StoragePosStock posStock =findStoragePosStockBySkuIdAndPositionId(skuId, positionId);
		if(posStock==null){
			throw new RuntimeException("库位库存明细为空");
		}
		if (posStock.getAmount() < amount) {
			throw new RuntimeException("库位出库量不能大于库位库存量");
		}
		posStock.setAmount(posStock.getAmount() - amount);
		updateStoragePosStock(posStock);
	}
	

	private void pamarterCheck(Integer skuId,Integer positionId, Integer amount) {
		if (skuId==null||skuId <=0) {
			throw new RuntimeException("skuId不能为NULL,0或者负数");
		}
		if (amount==null||amount <= 0) {
			throw new RuntimeException("变动数量不能为NULL,0或者负数");
		}
		if (positionId==null||positionId< 0 ||positionId == 0) {
			throw new RuntimeException("库位Id不能为NULL,0或者负数");
		}
	}

	
	
	protected StorageIoTask processFindIoTaskInstance(Integer taskId) {
		StorageIoTask storageIoTask =storageIoTaskDao.findById(taskId);
		if(storageIoTask==null){
			throw new RuntimeException("没有指定的库存变动操作");
		}
		return storageIoTask;
	}
	
	protected void parameterCheckUpdateStorageIoTask(Integer taskId,Integer amount){
		if (taskId==null||taskId < 0 || taskId == 0) {
			throw new RuntimeException("skuId不能为0或者负数");
		}
		if (amount==null||amount < 0 || amount == 0) {
			throw new RuntimeException("商品数量不能为0或者负数");
		}
	}
	
//	/**
//	 * 更新库存变动单明细<更新出库,配货>
//	 * updateStorageIoDetail(operateType==updateOutwarehouse)
//	 * @param taskId
//	 * @param positionId
//	 * @param skuId
//	 * @param amount
//	 */
//	public void updateStorageIodetailUpdateOutwarehouse(Integer taskId,Integer positionId,Integer skuId,Integer amount){
//		parameterCheckUpdateStorageIodetail(taskId,positionId, skuId, amount);
//		StorageIoDetail detail=processFindIodetaiInstance(taskId,positionId, skuId, amount);
//		detail.setAmount(detail.getAmount()+amount);
//		updateStorageIoDetail(detail);
//	}
	
	/**
	 * 更新库存变动单明细<出错标记变动明细,配货>
	 * updateStorageIoDetail()
	 * @param taskId
	 * @param positionId
	 * @param skuId
	 * @param amount
	 */
	public void updateStorageIoDetailErrorReturn(Integer taskId,Integer positionId,Integer skuId,Integer amount){
		parameterCheckUpdateStorageIodetail(taskId,positionId, skuId, amount);
		StorageIoDetail detail=processFindIodetaiInstance(taskId,positionId, skuId, amount);
		detail.setMemo("缺配数量："+amount);
		updateStorageIoDetail(detail);
	}
	
	private void parameterCheckUpdateStorageIodetail(Integer taskId,Integer positionId, Integer skuId, Integer amount) {
		if(taskId==null||taskId<=0){
			throw new IllegalArgumentException("taskId不能为空或者0和负数");
		}
		if(positionId==null||positionId<=0){
			throw new IllegalArgumentException("positionId不能为空或者0和负数");
		}
		if(skuId==null||skuId<=0){
			throw new IllegalArgumentException("skuId不能为空或者0和负数");
		}
		if(amount==null||amount<=0){
			throw new IllegalArgumentException("amount不能为空或者0和负数");
		}
	}

	private StorageIoDetail processFindIodetaiInstance(Integer taskId,
			Integer positionId, Integer skuId, Integer amount) {// updateStorageIoDetailProcess
		StorageIoDetail storageIoDetail = new StorageIoDetail();
		storageIoDetail.setTaskId(taskId);
		storageIoDetail.setSkuId(skuId);
		storageIoDetail.setPositionId(positionId);
		List<StorageIoDetail> list = storageIoDetailDao	.findByExample(storageIoDetail);
		if (list == null || list.isEmpty()) {
			throw new RuntimeException("无库存变动明细");
		}
		if (list.size() > 1) {
			throw new RuntimeException("变动明细对应多条记录");
		}
		return list.get(0);
	}
	
	protected void updateStorageIoTask(StorageIoTask task) {
		task.setGmtModify(nowTime());
		task.setVersion(task.getVersion() + 1);
		storageIoTaskDao.update(task);
	}
	
	protected void saveStorageIoDetail(StorageIoDetail storageIoDetail) {
		storageIoDetail.setGmtCreate(nowTime());
		storageIoDetail.setGmtModify(nowTime());
		storageIoDetail.setVersion(0);
		storageIoDetailDao.save(storageIoDetail);
	}

	private void updateStorageIoDetail(StorageIoDetail detail) {
		detail.setGmtModify(nowTime());
		detail.setVersion(detail.getVersion()+1);
		storageIoDetailDao.update(detail);
	}
	
	protected StoragePosition saveStoragePosition(StoragePosition instance) {
		instance.setPositionIsfull(false);
		instance.setPositionIslock(false);
		instance.setLockReason(null);
		instance.setGmtCreate(nowTime());
		instance.setGmtModify(nowTime());
		instance.setVersion(0);
		storagePositionDao.save(instance);
		return instance;
	}
	
	
	protected void saveStorageProduct(StorageProduct instance) {
		instance.setLockFlag(false);
		instance.setLockReason(null);
		instance.setGmtCreate(nowTime());
		instance.setGmtModify(nowTime());
		instance.setVersion(0);
		storageProductDao.save(instance);
		
	}
	
	protected void updateStoragePosStock(StoragePosStock posStock) {
		posStock.setGmtModify(nowTime());
		posStock.setVersion(posStock.getVersion() + 1);
		storagePosStockDao.update(posStock);
	}
	
	protected void updateStorageProduct(StorageProduct storageProduct) {
		storageProduct.setGmtModify(nowTime());
		storageProduct.setVersion(storageProduct.getVersion()+1);
		storageProductDao.update(storageProduct);
	}
	
	protected StorageIoTask saveStorageIoTask(StorageIoTask storageIoTask) {
		storageIoTask.setGmtCreate(nowTime());
		storageIoTask.setGmtModify(nowTime());
		storageIoTask.setVersion(0);
		storageIoTaskDao.save(storageIoTask);
		return storageIoTask;
	}
	
	protected void updateStoragePosition(StoragePosition instance) {
		instance.setGmtModify(nowTime());
		instance.setVersion(instance.getVersion()+1);
		storagePositionDao.update(instance);
	}
	
	protected void saveStoragePosStock(StoragePosStock storagePosStock) {
		storagePosStock.setGmtCreate(nowTime());
		storagePosStock.setGmtModify(nowTime());
		storagePosStock.setVersion(0);
		storagePosStockDao.save(storagePosStock);
	}
	
	protected void saveCheckPlane(StorageCheckPlan checkPlan) {
		checkPlan.setGmtCreate(nowTime());
		checkPlan.setGmtModify(nowTime());
		checkPlan.setVersion(0);
		storageCheckPlanDao.save(checkPlan);
	}
	

	protected void saveStorageCheck(StorageCheck storageCheck) {
		storageCheck.setGmtCreate(nowTime());
		storageCheck.setGmtModify(nowTime());
		storageCheck.setVersion(0);
		storageCheckDao.save(storageCheck);
	}
	
	protected void saveStorageCheckSnap(StorageCheckSnap storageCheckSnap) {
		storageCheckSnap.setGmtCreate(nowTime());
		storageCheckSnap.setGmtModify(nowTime());
		storageCheckSnap.setIsdelete(false);
		storageCheckSnap.setVersion(0);
		storageCheckSnapDao.save(storageCheckSnap);
	}
	
	protected void updateStorageCheckSnap(StorageCheckSnap instance) {
		instance.setGmtModify(nowTime());
		instance.setVersion(instance.getVersion());
		storageCheckSnapDao.update(instance);
		
	}

	protected void saveStorageCheckDiffAdjust(StorageCheckDiffAdjust instance) {
		instance.setGmtCreate(nowTime());
		instance.setGmtModify(nowTime());
		instance.setVersion(0);
		storageCheckDiffAdjustDao.save(instance);
	}

	protected void updateStorageCheck(StorageCheck instance) {
		instance.setGmtModify(nowTime());
		instance.setVersion(instance.getVersion()+1);
		storageCheckDao.update(instance);
	}
		
	protected void saveStorageCheckDiff(StorageCheckDiff checkDiff) {
		checkDiff.setGmtCreate(nowTime());
		checkDiff.setGmtModify(nowTime());
		checkDiff.setVersion(0);
		storageCheckDiffDao.save(checkDiff); 
	}
	

	protected void updateStorageCheckDiff(StorageCheckDiff checkDiff) {
		checkDiff.setGmtModify(nowTime());
		checkDiff.setVersion(checkDiff.getVersion()+1);
		storageCheckDiffDao.update(checkDiff);
	}
	
	protected void updateStorageCheckPlan(StorageCheckPlan checkPlan) {
		checkPlan.setGmtModify(nowTime());
		checkPlan.setVersion(checkPlan.getVersion()+1);
		storageCheckPlanDao.update(checkPlan);
	}

	/**
	 * findStorageProductBySkuId
	 * @param skuId 
	 * @return StorageProduct
	 */
	public StorageProduct findStorageProductBySkuId(Integer skuId){
		DetachedCriteria criteria = DetachedCriteria.forClass(StorageProduct.class);
    	criteria.add(Restrictions.eq("skuId", skuId));
    	return findUniqueRecord(storageProductDao.findByCriteria(criteria));
	}
	
	/**
	 * findStorageProductListBySkuId;
	 * @param skuId 
	 * @return List<StorageProduct>
	 */
	public List<StorageProduct> findStorageProductListBySkuId(Integer skuId){
		DetachedCriteria criteria = DetachedCriteria.forClass(StorageProduct.class);
    	criteria.add(Restrictions.eq("skuId", skuId));
    	return storageProductDao.findByCriteria(criteria);
	}
	
	/**
	 * findStoragePosStockBySkuIdAndPositionId 
	 * @param skuId
	 * @param positionId
	 * @return  List<StoragePosStock>
	 */
    public StoragePosStock findStoragePosStockBySkuIdAndPositionId(Integer skuId,Integer positionId){
    	DetachedCriteria criteria = DetachedCriteria.forClass(StoragePosStock.class);
    	criteria.add(Restrictions.eq("skuId", skuId));
    	criteria.add(Restrictions.eq("positionId", positionId));
    	return findUniqueRecord(storagePosStockDao.findByCriteria(criteria));
    }
    
    /**
     * findStorageIoDetailByTaskId
     * @param taskId
     * @return List<StorageIoDetail>
     */
    public List<StorageIoDetail> findStorageIoDetailByTaskId(Integer taskId){
    	DetachedCriteria criteria = DetachedCriteria.forClass(StorageIoDetail.class);
    	criteria.add(Restrictions.eq("taskId", taskId));
    	return storageIoDetailDao.findByCriteria(criteria);
    }
    
    /**
     * findBySkuIdOrderByPositionIdAndAmount 配货查询
     * @param skuId
     * @param positionId
     * @param amount
     * @return List<StoragePosStock>
     */
    public List<StoragePosStock> findBySkuIdOrderByPositionIdAndAmount(Integer skuId){
    	DetachedCriteria criteria = DetachedCriteria.forClass(StoragePosStock.class);
    	criteria.add(Restrictions.eq("skuId", skuId));
    	criteria.add(Restrictions.gt("amount", 0));
    	criteria.addOrder(Order.desc("amount"));
    	return storagePosStockDao.findByCriteria(criteria);
    }
    
    /**
     * findStoragePositionByPositionLabel 
     * @param positionLabel
     * @return List<StoragePosition> 
     */
    public StoragePosition findStoragePositionByPositionLabel(String positionLabel){
    	DetachedCriteria criteria = DetachedCriteria.forClass(StoragePosition.class);
    	criteria.add(Restrictions.eq("positionLabel", positionLabel));
    	return findUniqueRecord(storagePositionDao.findByCriteria(criteria));
    }
    
    /**
     * findStorageCheckDiffByCheckId 
     * @param checkId
     * @return List<StorageCheckDiff>
     */
    public List<StorageCheckDiff> findStorageCheckDiffByCheckId(Integer checkId){
    	DetachedCriteria criteria = DetachedCriteria.forClass(StorageCheckDiff.class);
    	criteria.add(Restrictions.eq("checkId", checkId));
    	return storageCheckDiffDao.findByCriteria(criteria);
    }
    
    /**
     * findStorageCheckDiffByCheckIdAndSkuId
     * @param checkId
     * @param skuId
     * @return StorageCheckDiff
     */
    public StorageCheckDiff findStorageCheckDiffByCheckIdAndSkuId(int checkId, int skuId) {
    	DetachedCriteria criteria = DetachedCriteria.forClass(StorageCheckDiff.class);
    	criteria.add(Restrictions.eq("checkId", checkId));
    	criteria.add(Restrictions.eq("skuId", skuId));
    	return findUniqueRecord(storageCheckDiffDao.findByCriteria(criteria));
	}
    
    /**
     * findStorageCheckSnapByCheckId 
     * @param checkId
     * @return List<StorageCheckDiff>
     */
    public List<StorageCheckSnap> findStorageCheckSnapByCheckId(Integer checkId){
    	DetachedCriteria criteria = DetachedCriteria.forClass(StorageCheckSnap.class);
    	criteria.add(Restrictions.eq("checkId", checkId));
    	return storageCheckSnapDao.findByCriteria(criteria);
    }
    
    /**
     * findByCheckIdAndCheckTime StorageCheckSnap
     * @param checkId
     * @param checkTime
     * @return  List<StorageCheckSnap> 
     */
    public List<StorageCheckSnap> findByCheckIdAndCheckTime(Integer checkId,String checkTime){
    	DetachedCriteria criteria = DetachedCriteria.forClass(StorageCheckSnap.class);
    	criteria.add(Restrictions.eq("isdelete", false));
    	criteria.add(Restrictions.eq("checkId", checkId));
    	criteria.add(Restrictions.eq("checkTime", checkTime));
    	return storageCheckSnapDao.findByCriteria(criteria);
    }
    
    /**
     * findStoragePosStockByPositionId
     * @param positionId
     * @return List<StoragePosStock>
     */
    public List<StoragePosStock> findStoragePosStockByPositionId(Integer positionId) {
		DetachedCriteria criteria = DetachedCriteria.forClass(StoragePosStock.class);
    	criteria.add(Restrictions.eq("positionId", positionId));
    	criteria.add(Restrictions.gt("amount", 0));
    	return storagePosStockDao.findByCriteria(criteria);
	}
    
    /**
     * findCheckSizeByCheckPlanId
     * @param checkPlanId
     * @return StorageCheck
     */
	public List<StorageCheck> findCheckByCheckPlanId(int checkPlanId) {
		DetachedCriteria criteria = DetachedCriteria.forClass(StorageCheck.class);
    	criteria.add(Restrictions.eq("checkPlanId", checkPlanId));
    	return storageCheckDao.findByCriteria(criteria);
	}
	
	/**
	 * findCheckPlanByCheckPlanId
	 * @param checkPlanId
	 * @return StorageCheckPlan
	 */
	public StorageCheckPlan findCheckPlanByCheckPlanId(int checkPlanId) {
		DetachedCriteria criteria = DetachedCriteria.forClass(StorageCheckPlan.class);
    	criteria.add(Restrictions.eq("checkPlanId", checkPlanId));
		return findUniqueRecord(storageCheckPlanDao.findByCriteria(criteria));
	}
	
    private <T> T findUniqueRecord(List<T> list){
    	if(list==null||list.isEmpty()){
    		return null;
    	}
    	if(list.size()>1){
    		throw new RuntimeException("存在多条记录！！");
    	}
    	return list.get(0);
    }

}
   