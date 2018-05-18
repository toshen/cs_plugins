package com.cloud.vm;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.dao.UserVmDao;
import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.api.command.DeployHiddenVMCmd;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class UserVmServiceModified extends UserVmManagerImpl {

    @Inject
    protected UserVmDao _vmDao;
    private static final Logger s_logger = Logger.getLogger(UserVmServiceModified.class);

    public UserVmServiceModified() {
        super();
    }

    @PostConstruct
    public UserVm startVirtualMachine(DeployHiddenVMCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException {
        return this.startVirtualMachine(cmd, (Map)null, cmd.getDeploymentPlanner());
    }

    protected UserVm startVirtualMachine(DeployHiddenVMCmd cmd, Map<VirtualMachineProfile.Param, Object> additonalParams, String deploymentPlannerToUse) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException {
        long vmId = cmd.getEntityId();
        Long hostId = cmd.getHostId();

        UserVmVO vm = (UserVmVO)this._vmDao.findById(vmId);
        Pair vmParamPair = null;

        try {
            vmParamPair = this.startVirtualMachine(vmId, hostId, additonalParams, deploymentPlannerToUse);
            vm = (UserVmVO)vmParamPair.first();
            UserVmVO tmpVm = (UserVmVO)this._vmDao.findById(vm.getId());
            if (!tmpVm.getState().equals(VirtualMachine.State.Running)) {
                s_logger.error("VM " + tmpVm + " unexpectedly went to " + tmpVm.getState() + " state");
                throw new ConcurrentOperationException("Failed to deploy VM " + vm);
            }
        } finally {
            this.updateVmStateForFailedVmCreation(vm.getId(), hostId);
        }

        VMTemplateVO var13 = (VMTemplateVO)this._templateDao.findByIdIncludingRemoved(vm.getTemplateId());
        if (var13.getEnablePassword()) {
            vm.setPassword((String)((Map)vmParamPair.second()).get(VirtualMachineProfile.Param.VmPassword));
        }

        return vm;
    }

    private void updateVmStateForFailedVmCreation(Long vmId, Long hostId) {
        UserVmVO vm = (UserVmVO)this._vmDao.findById(vmId);
        if (vm != null && vm.getState().equals(VirtualMachine.State.Stopped)) {
            s_logger.debug("Destroying vm " + vm + " as it failed to create on Host with Id:" + hostId);

            try {
                this._itMgr.stateTransitTo(vm, VirtualMachine.Event.OperationFailedToError, (Long)null);
            } catch (NoTransitionException var7) {
                s_logger.warn(var7.getMessage());
            }

            List<VolumeVO> volumesForThisVm = this._volsDao.findUsableVolumesForInstance(vm.getId());
            Iterator var5 = volumesForThisVm.iterator();

            while(var5.hasNext()) {
                VolumeVO volume = (VolumeVO)var5.next();
                if (volume.getState() != com.cloud.storage.Volume.State.Destroy) {
                    this.volumeMgr.destroyVolume(volume);
                }
            }

            String msg = "Failed to deploy Vm with Id: " + vmId + ", on Host with Id: " + hostId;
            this._alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_USERVM, vm.getDataCenterId(), vm.getPodIdToDeployIn(), msg, msg);
            ServiceOfferingVO offering = this._serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());
            this.resourceCountDecrement(vm.getAccountId(), vm.isDisplayVm(), new Long((long)offering.getCpu()), new Long((long)offering.getRamSize()));
        }

    }

}
