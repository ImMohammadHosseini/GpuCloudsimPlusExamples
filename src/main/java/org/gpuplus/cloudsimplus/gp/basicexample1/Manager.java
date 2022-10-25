package org.gpuplus.cloudsimplus.gp.basicexample1;

import org.cloudbus.cloudsim.gp.cloudlets.GpuCloudlet;
import org.cloudbus.cloudsim.gp.cloudlets.GpuCloudletSimple;
import org.cloudbus.cloudsim.gp.datacenters.GpuDatacenter;
import org.cloudbus.cloudsim.gp.datacenters.GpuDatacenterSimple;
import org.cloudbus.cloudsim.gp.brokers.GpuDatacenterBroker;
import org.cloudbus.cloudsim.gp.brokers.GpuDatacenterBrokerSimple;
import org.cloudbus.cloudsim.gp.vms.GpuVm;
import org.cloudbus.cloudsim.gp.vms.GpuVmSimple;
//import org.cloudbus.cloudsim.gp.vgpu.VGpu;
import org.cloudbus.cloudsim.gp.vgpu.VGpuSimple;
import org.cloudbus.cloudsim.gp.hosts.GpuHost;
import org.cloudbus.cloudsim.gp.hosts.GpuHostSimple;
import org.cloudbus.cloudsim.gp.resources.GpuCore;
import org.cloudbus.cloudsim.gp.resources.GpuCoreSimple;
import org.cloudbus.cloudsim.gp.resources.Gpu;
import org.cloudbus.cloudsim.gp.resources.GpuSimple;
//import org.cloudbus.cloudsim.gp.cloudlets.gputasks.GpuTask;
import org.cloudbus.cloudsim.gp.cloudlets.gputasks.GpuTaskSimple;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
//import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;

import java.util.ArrayList;
import java.util.List;

public class Manager 
{
	private static final int  HOSTS = 1;
    private static final int  HOST_PES = 8;
    private static final int  HOST_GPUS = 4;
    private static final int  HOST_MIPS = 1000;
    private static final int  HOST_RAM = 2048; //in Megabytes
    private static final long HOST_BW = 10_000; //in Megabits/s
    private static final long HOST_STORAGE = 1_000_000; //in Megabytes
    
    private static final int  GPU_CORES = 500;
    private static final int  GPU_MIPS = 500;
    
    private static final int VMS = 2;
    private static final int VM_PES = 4;
    private static final int VGPU_CORES = 200;
    
    private static final int CLOUDLETS = 4;
    private static final int CLOUDLET_PES = 2;
    private static final int CLOUDLET_LENGTH = 10_000;
    
    private static final int GPUTASK_PES = 2;
    private static final int GPUTASK_LENGTH = 10_000;

    private CloudSim simulation;
    private GpuDatacenterBroker broker0;
    private List<GpuVm> vmList;
    private List<GpuCloudlet> cloudletList;
    private GpuDatacenter datacenter0;
    
    
    public Manager () {
    	simulation = new CloudSim();
    	datacenter0 = createDatacenter();
    	
    	broker0 = new GpuDatacenterBrokerSimple(simulation);

        vmList = createVms();
        cloudletList = createCloudlets();
        broker0.submitVmList(vmList);
        broker0.submitCloudletList(cloudletList);
        
        simulation.start();

        final List<GpuCloudlet> finishedCloudlets = broker0.getCloudletFinishedList();
        new CloudletsTableBuilder(finishedCloudlets).build();
        System.out.println("hi");
    }
    
    private GpuDatacenter createDatacenter () {
    	final var hostList = new ArrayList<GpuHost>(HOSTS);
        for(int i = 0; i < HOSTS; i++) {
            final var host = createHost();
            hostList.add(host);
        }

        //Uses a VmAllocationPolicySimple by default to allocate VMs
        return new GpuDatacenterSimple(simulation, hostList);
    }
    
    private GpuHost createHost () {
    	final var peList = new ArrayList<Pe>(HOST_PES);
    	final var gpuList = new ArrayList<Gpu>(HOST_GPUS);
    	
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < HOST_PES; i++) {
            //Uses a PeProvisionerSimple by default to provision PEs for VMs
            peList.add(new PeSimple(HOST_MIPS));
        }
        for (int i = 0; i < HOST_GPUS; i++) {
        	gpuList.add(createGpu());
        }
        /*
        Uses ResourceProvisionerSimple by default for RAM and BW provisioning
        and VmSchedulerSpaceShared for VM scheduling.
        */
        return new GpuHostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, peList, gpuList);
    }
    
    private GpuSimple createGpu () {
    	final var coreList = new ArrayList<GpuCore>(GPU_CORES);
    	
    	for (int i = 0; i < GPU_CORES; i++) {
    		coreList.add(new GpuCoreSimple(GPU_MIPS));
        }
    	return new GpuSimple((coreList), true);
    }
    
    private List<GpuVm> createVms () {
    	final var vmList = new ArrayList<GpuVm>(VMS);
        for (int i = 0; i < VMS; i++) {
        	var vgpu = new VGpuSimple(GPU_MIPS, VGPU_CORES);
        	vgpu.setGddram(512).setBw(1000);
            //Uses a CloudletSchedulerTimeShared by default to schedule Cloudlets
        	final double DOUBLE_HOST_MIPS = HOST_MIPS;
        	long LONG_VM_PES = VM_PES;
            var vm = new GpuVmSimple(DOUBLE_HOST_MIPS, LONG_VM_PES, vgpu, "");
            vm.setRam(512).setBw(1000).setSize(10_000);
            vmList.add(vm);
        }

        return vmList;
    }
    
    private List<GpuCloudlet> createCloudlets () {
    	var cloudletList = new ArrayList<GpuCloudlet>(CLOUDLETS);

        //UtilizationModel defining the Cloudlets use only 50% of any resource all the time
        var utilizationModel = new UtilizationModelDynamic(0.5);

        for (int i = 0; i < CLOUDLETS; i++) {
        	var gputask = new GpuTaskSimple(GPUTASK_LENGTH, GPUTASK_PES, utilizationModel);
            var cloudlet = new GpuCloudletSimple(CLOUDLET_LENGTH, CLOUDLET_PES, utilizationModel,
            		gputask);
            cloudlet.setSizes(1024);
            cloudletList.add(cloudlet);
        }

        return cloudletList;
    }
}
