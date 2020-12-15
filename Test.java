package com.offcn.project.controller;

import com.alibaba.fastjson.JSON;
import com.offcn.common.AppResponse;
import com.offcn.project.contants.ProjectConstant;
import com.offcn.project.enums.ProjectStatusEnume;
import com.offcn.project.pojo.*;
import com.offcn.project.service.ProjectCreateService;
import com.offcn.project.vo.req.*;
import com.offcn.vo.BaseVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/project")
@Api(tags = "������Ŀ")
@Slf4j
public class ProjectCreateController {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProjectCreateService projectCreateService;

    @PostMapping("/init")
    @ApiOperation("������Ŀ�ĵ�һ��--������Ŀ,ͬ��Э��")
    public AppResponse<Object> init(BaseVo baseVo){
        String accessToken = baseVo.getAccessToken();
        String memberId = redisTemplate.opsForValue().get(accessToken);
        if (memberId == null){
            return AppResponse.fail("��Ȩ�޲���,���ȵ�¼");
        }
        String projectToken = projectCreateService.initCreateProject(Integer.parseInt(memberId));
        return AppResponse.ok(projectToken);
    }
    @ApiOperation("������Ŀ�ĵڶ���--�ֻ�����Ŀ������Ϣ")
    @PostMapping("/saveBaseInfo")
    public AppResponse<Object> saveBaseInfo(@RequestBody ProjectBaseInfoVo baseInfoVo){

        String projectContext = redisTemplate.opsForValue().get(ProjectConstant.TEMP_PROJECT_PREFIX+baseInfoVo.getProjectToken());
        ProjectRedisStorageVo redisStorageVo = JSON.parseObject(projectContext,ProjectRedisStorageVo.class);
        BeanUtils.copyProperties(baseInfoVo,redisStorageVo);
        String jsonStr = JSON.toJSONString(redisStorageVo);
        redisTemplate.opsForValue().set(ProjectConstant.TEMP_PROJECT_PREFIX+baseInfoVo.getProjectToken(),jsonStr);
        return AppResponse.ok("�ռ�ҳ����Ŀ��Ϣ�ɹ�");
    }
    @ApiOperation("������Ŀ�ĵ�����--�ռ���Ŀ�ر�������Ϣ")
    @PostMapping("/saveReturnInfo")
    public AppResponse<Object> saveReturnInfo(@RequestBody List<ProjectReturnVo> returnVoList){
        String projectToken = returnVoList.get(0).getProjectToken();
        String projectContext = redisTemplate.opsForValue().get(ProjectConstant.TEMP_PROJECT_PREFIX+projectToken);
        ProjectRedisStorageVo redisStorageVo = JSON.parseObject(projectContext,ProjectRedisStorageVo.class);
        List<TReturn> returnList = new ArrayList<>();
        for (ProjectReturnVo returnVo : returnVoList) {
            TReturn tReturn = new TReturn();
            BeanUtils.copyProperties(returnVo,tReturn);
            returnList.add(tReturn);
        }
        redisStorageVo.setProjectReturns(returnList);
        String jsonStr = JSON.toJSONString(redisStorageVo);
        redisTemplate.opsForValue().set(ProjectConstant.TEMP_PROJECT_PREFIX+projectToken,jsonStr);
        return AppResponse.ok("�ռ���Ŀ�㱨������Ϣ�ɹ�");
    }
    @ApiOperation("������Ŀ�ĵ��Ĳ�--������Ŀ��Ϣ")
    @ApiImplicitParams({@ApiImplicitParam(name = "accessToken",value = "��½����",required = true),
                        @ApiImplicitParam(name = "projectToken",value = "��Ŀ����",required = true),
                        @ApiImplicitParam(name = "ops",value = "�û��������� 0-����ݸ� 1-�ύ���",required = true)})
    @PostMapping("/saveProjectInfo")
    public AppResponse saveProjectInfo(String accessToken,String projectToken,String ops){
        String memberId = redisTemplate.opsForValue().get(accessToken);
        if (memberId==null){
            return AppResponse.fail("�޴�Ȩ��,���ȵ�¼");
        }
        String projectContext = redisTemplate.opsForValue().get(ProjectConstant.TEMP_PROJECT_PREFIX+projectToken);
        ProjectRedisStorageVo redisStorageVo = JSON.parseObject(projectContext,ProjectRedisStorageVo.class);
        if (!StringUtils.isEmpty(ops)){
            if (ops.equals("1")){
                projectCreateService.saveProjectInfo(ProjectStatusEnume.SUBMIT_AUTH,redisStorageVo);
                return AppResponse.ok("������Ŀ�ɹ�,���ύ���");
            }else if (ops.equals("0")){
                projectCreateService.saveProjectInfo(ProjectStatusEnume.DRAFT,redisStorageVo);
                return AppResponse.ok("��Ŀ���浽�ݸ�״̬");
            }else {
                return AppResponse.fail("��֧�ִ˲���");
            }
        }
        return AppResponse.fail(null);
    }

    @GetMapping("getReturnById/{projectId}")
    public AppResponse<List<TReturn>> getReturnById(@PathVariable Integer projectId){

        try {
            List<TReturn> list = projectCreateService.getReturnList(projectId);
            return AppResponse.ok(list);


        } catch (Exception e) {
            e.printStackTrace();
            return AppResponse.fail(null);
        }
    }
    @GetMapping("allProject")
    public AppResponse<List<ProjectVo>> allProject(){

        try {
            List<TProject> list = projectCreateService.findAllProject();
            List<ProjectVo> returnList = new ArrayList<>();
            for (TProject tProject : list) {
                ProjectVo pv = new ProjectVo();
                BeanUtils.copyProperties(tProject,pv);
                Integer id = tProject.getId();
                //������Ŀ��Ż�ȡ��Ŀ��ͼ
                List<TProjectImages> images = projectCreateService.getProjectImages(id);

                //������Ŀ��ͼ����
                for (TProjectImages tProjectImages : images) {
                    //���ͼƬ������ͷ��ͼƬ��������ͷ��ͼƬ·������ĿVO
                    if (tProjectImages.getImgtype() == 0) {
                        pv.setHeaderImage(tProjectImages.getImgurl());
                    }
                }
                returnList.add(pv);
            }
            return AppResponse.ok(returnList);


        } catch (Exception e) {
            e.printStackTrace();
            return AppResponse.fail(null);
        }
    }
    @ApiOperation("��Ŀ����id��ѯ������Ϣ")
    @GetMapping("projectDetail/{projectId}")
    public AppResponse<ProjectDetailVo> projectDetail(@PathVariable Integer projectId){

        try {
            ProjectDetailVo vo = new ProjectDetailVo();
            TProject project = projectCreateService.findById(projectId);
            BeanUtils.copyProperties(project,vo);
            List<TProjectImages> images = projectCreateService.getProjectImages(projectId);
            List<String> imgList = new ArrayList<>();
            for (TProjectImages img : images) {
                if (img.getImgtype()==0){
                    vo.setHeaderImage(img.getImgurl());
                }
                imgList.add(img.getImgurl());
            }
            vo.setDetailsImage(imgList);
            List<TReturn> returnList = projectCreateService.getReturnList(projectId);
            vo.setProjectReturns(returnList);
            return AppResponse.ok(vo);

        } catch (Exception e) {
            e.printStackTrace();
            return AppResponse.fail(null);
        }
    }
    @GetMapping("getAllTags")
    public AppResponse<List<TTag>> getAllTags(){

        try {
            List<TTag> list = projectCreateService.getAllTags();
            return AppResponse.ok(list);

        } catch (Exception e) {
            e.printStackTrace();
            return AppResponse.fail(null);
        }
    }
    @GetMapping("getAllType")
    public AppResponse<List<TType>> getAllType(){

        try {
            List<TType> list = projectCreateService.getAllType();
            return AppResponse.ok(list);

        } catch (Exception e) {
            e.printStackTrace();
            return AppResponse.fail(null);
        }
    }
    @ApiOperation("��Ŀ�㱨����ID��ѯ������Ϣ")
    @GetMapping("getReturnDetail/{returnId}")
    public AppResponse<TReturn> getReturnDetail(@PathVariable Integer returnId){
        try {
            TReturn returnEntity = projectCreateService.getReturnById(returnId);
            return AppResponse.ok(returnEntity);
        } catch (Exception e) {
            e.printStackTrace();
            return AppResponse.fail(null);
        }
    }
}








