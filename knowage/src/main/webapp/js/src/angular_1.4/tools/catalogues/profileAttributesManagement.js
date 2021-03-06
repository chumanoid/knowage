/**
 *
 */
var app=angular.module('profileAttributesManagementModule',['ngMaterial', 'ngMessages', 'angular_list','angular_table','sbiModule','angular_2_col','angular-list-detail', 'angularXRegExp']);
app.config(['$mdThemingProvider', function($mdThemingProvider) {
    $mdThemingProvider.theme('knowage')
    $mdThemingProvider.setDefaultTheme('knowage');
 }]);
app.controller('profileAttributesManagementController',["sbiModule_translate","sbiModule_restServices", "kn_regex", "$scope","$mdDialog","$mdToast","$timeout","sbiModule_messaging",profileAttributesManagementFunction]);

function profileAttributesManagementFunction(sbiModule_translate,sbiModule_restServices,kn_regex,$scope,$mdDialog,$mdToast,$timeout,sbiModule_messaging){

	$scope.regex = kn_regex;
	$scope.translate=sbiModule_translate;
	$scope.showMe=false;
	$scope.dirtyForm=false;
	$scope.tempObj=[];
	$scope.selectedAttribute={};
	$scope.attributeList=[];
	$scope.tempObj = [];
	$scope.lovs = [];
	$scope.enumAsArrayOfObjects = enumAsArrayOfObjects;
	$scope.lovIdAndColumns = [];
	$scope.lovColumnsId = [];
	$scope.disableLov = false;

	angular.element(document).ready(function () {
        $scope.getProfileAttributes();
        $scope.getLovs();
    });


	$scope.setDirty=function(atr){
		$scope.dirtyForm=true;

	}

	$scope.getProfileAttributes= function(){
			sbiModule_restServices.promiseGet("2.0/attributes", '')
			.then(function(response) {
				$scope.attributeList = response.data;
				prepareForMultiselect($scope.attributeList)
			}, function(response) {
				sbiModule_messaging.showErrorMessage(response.data.errors[0].message, 'Error');

			});

	}
    var prepareForMultiselect = function(attributes){
    	for(var i = 0; i<attributes.length; i++){
    		if(attributes[i].lovColumns){
    			attributes[i].lovColumns = attributes[i].lovColumns.split(',')
    		}
    	}
}
	$scope.getLovs= function(){

		sbiModule_restServices.promiseGet("2.0/lovs", 'get/all')
		.then(function(response) {
			$scope.lovs = response.data;
		}, function(response) {
			sbiModule_messaging.showErrorMessage(response.data.errors[0].message, 'Error');

		});
}
	$scope.getLovsValues = function(lovId){
		var lovIdAndColumns = {}
		var columns = []
		sbiModule_restServices.promiseGet("2.0/lovs", lovId+'/preview')
		.then(function(response){
			lovIdAndColumns.id = lovId ;
			lovIdAndColumns.columns = response.data;
			$scope.lovIdAndColumns.push(lovIdAndColumns);
		})
	}

	$scope.createProfileAttribute=function(){

		if($scope.dirtyForm){
			$mdDialog.show($scope.confirm).then(function(){

     		    $scope.dirtyForm=false;
     		   $scope.selectedAttribute={};
				$scope.showMe=true;

			},function(){

				$scope.showMe=true;
			});

		}else{

			$scope.showMe=true;
			$scope.selectedAttribute={};
		}

	}

	$scope.saveProfileAttribute=function(){
		if($scope.selectedAttribute.value)
		$scope.selectedAttribute.value = ($scope.enumAsArrayOfObjects.filter(type => type.name == $scope.selectedAttribute.value.name )[0].name);
		if($scope.selectedAttribute.hasOwnProperty("attributeId")){ // put,
																	// update
																	// existing

				if(!$scope.disableLov || $scope.selectedAttribute.lovId == "" ){
				   $scope.selectedAttribute.lovId = undefined;
				}

			sbiModule_restServices.promisePut('2.0/attributes',$scope.selectedAttribute.attributeId,$scope.selectedAttribute)
			.then(function(response) {

				for(i=0;i<$scope.attributeList.length;i++){
					if($scope.attributeList[i].attributeId===$scope.selectedAttribute.attributeId){
						$scope.attributeList[i].attributeName=$scope.selectedAttribute.attributeName;
						$scope.attributeList[i].attributeDescription=$scope.selectedAttribute.attributeDescription;
						$scope.attributeList[i].allowUser = $scope.selectedAttribute.allowUser;
						$scope.attributeList[i].syntax = $scope.selectedAttribute.syntax;
						$scope.attributeList[i].multivalue = $scope.selectedAttribute.multivalue;
						if($scope.selectedAttribute.lovId != "" || !$scope.disableLov){
						$scope.attributeList[i].lovId = $scope.selectedAttribute.lovId;
						} else $scope.attributeList[i].lovId = undefined
						if($scope.selectedAttribute.value)
						$scope.selectedAttribute.value = ($scope.enumAsArrayOfObjects.filter(type => type.name == $scope.selectedAttribute.value )[0]);

					}
				}
				$scope.dirtyForm=false;
				sbiModule_messaging.showSuccessMessage(sbiModule_translate.load("sbi.catalogues.toast.updated"), 'Success!');
			}, function(response) {
				sbiModule_messaging.showErrorMessage(response.data.errors[0].message, 'Error');

			});

		}else{// post create new

			sbiModule_restServices.promisePost('2.0/attributes','',$scope.selectedAttribute)
			.then(function(response) {
				$scope.attributeList.push(response.data);
		        $scope.dirtyForm=false;
				sbiModule_messaging.showSuccessMessage(sbiModule_translate.load("sbi.catalogues.toast.created"), 'Success!');

			}, function(response) {
				sbiModule_messaging.showErrorMessage(response.data.errors[0].message, 'Error');

			});
		}

	}

	$scope.loadAttribute=function(item){
		if(angular.isNumber(item.lovId)){
			$scope.disableLov = true;
		}else $scope.disableLov = false;

		if($scope.dirtyForm){
			$mdDialog.show($scope.confirm).then(function(){

     		    $scope.dirtyForm=false;
				$scope.selectedAttribute=angular.copy(item);
				$scope.showMe=true;

			},function(){

				$scope.showMe=true;
			});

		}else{

		$scope.selectedAttribute=angular.copy(item);

		$scope.showMe=true;
		}
	}

	$scope.cancel=function(){
		$scope.selectedAttribute={};
		$scope.showMe=false;
		$scope.dirtyForm=false;
	}

	$scope.deleteItem=function(item){

		sbiModule_restServices.promiseDelete('2.0/attributes',item.attributeId)
		.then(function(response) {

			sbiModule_messaging.showSuccessMessage(sbiModule_translate.load("sbi.catalogues.toast.deleted"), 'Success!');
			$scope.attributeList=[];
			$timeout(function(){
				$scope.getProfileAttributes();
			}, 1000);
			$scope.selectedAttribute={};
			$scope.showMe=false;

		}, function(response) {
			sbiModule_messaging.showErrorMessage(sbiModule_translate.load("sbi.catalogues.error.inuse"), 'Error');

		});
	}

	$scope.deleteAttributes=function(){

		params="?";
		for(i=0;i<$scope.tempObj.length;i++){
			params+="id="+$scope.tempObj[i].attributeId+"&";
		}

		sbiModule_restServices.delete('2.0/attributes',params).success(
				function(data, status, headers, config){

					$scope.tempObj=[];
					$scope.attributeList=[];
					$timeout(function(){
						$scope.getProfileAttributes();
					}, 1000);
					$scope.selectedAttribute={};
					$scope.showMe=false;
					$scope.showActionOK();

				}).error(function(data,status,headers,config){

				})

	}


	$scope.paMenu=[
	               {
	            	  label:'delete',
	            	  action:function(item,event){

	            	  }
	               }
	               ];

	$scope.paSpeedMenu= [
	                     {
	                    	label:'delete',
	                    	icon:'fa fa-trash',
	                    	// icon:'fa fa-trash-o fa-lg',
	                    	// color:'#153E7E',
	                    	action:function(item,event){

	                    		$scope.confirmDelete(item,event);
	                    	}
	                     }
	                    ];

	 $scope.confirm = $mdDialog
	.confirm()
	.title(sbiModule_translate.load("sbi.catalogues.generic.modify"))
	.content(
			sbiModule_translate
			.load("sbi.catalogues.generic.modify.msg"))
			.ariaLabel('Lucky day').ok(
					sbiModule_translate.load("sbi.general.yes")).cancel(
							sbiModule_translate.load("sbi.general.No"));

	 $scope.confirmDelete = function(item,ev) {
		    var confirm = $mdDialog.confirm()
		          .title(sbiModule_translate.load("sbi.catalogues.toast.confirm.title"))
		          .content(sbiModule_translate.load("sbi.catalogues.toast.confirm.content"))
		          .ariaLabel("confirm_delete")
		          .targetEvent(ev)
		          .ok(sbiModule_translate.load("sbi.general.continue"))
		          .cancel(sbiModule_translate.load("sbi.general.cancel"));
		    $mdDialog.show(confirm).then(function() {
		    	$scope.deleteItem(item);
		    }, function() {

		    });
		  };


}