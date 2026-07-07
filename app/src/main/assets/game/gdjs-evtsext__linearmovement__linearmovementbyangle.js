
gdjs.evtsExt__LinearMovement__LinearMovementByAngle = gdjs.evtsExt__LinearMovement__LinearMovementByAngle || {};

/**
 * Behavior generated from Linear movement by angle
 */
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle = class LinearMovementByAngle extends gdjs.RuntimeBehavior {
  constructor(instanceContainer, behaviorData, owner) {
    super(instanceContainer, behaviorData, owner);
    this._runtimeScene = instanceContainer;

    this._onceTriggers = new gdjs.OnceTriggers();
    this._behaviorData = {};
    this._sharedData = gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.getSharedData(
      instanceContainer,
      behaviorData.name
    );
    
    this._behaviorData.Speed = behaviorData.Speed !== undefined ? behaviorData.Speed : Number("200") || 0;
  }

  // Hot-reload:
  applyBehaviorOverriding(behaviorOverriding) {
    
    if (behaviorOverriding.Speed !== undefined)
      this._behaviorData.Speed = behaviorOverriding.Speed;

    return true;
  }

  // Network sync:
  getNetworkSyncData(syncOptions) {
    return {
      ...super.getNetworkSyncData(syncOptions),
      props: {
        
    Speed: this._behaviorData.Speed,
      }
    };
  }
  updateFromNetworkSyncData(networkSyncData, options) {
    super.updateFromNetworkSyncData(networkSyncData, options);
    
    if (networkSyncData.props.Speed !== undefined)
      this._behaviorData.Speed = networkSyncData.props.Speed;
  }

  // Properties:
  
  _getSpeed() {
    return this._behaviorData.Speed !== undefined ? this._behaviorData.Speed : Number("200") || 0;
  }
  _setSpeed(newValue) {
    this._behaviorData.Speed = newValue;
  }
}

/**
 * Shared data generated from Linear movement by angle
 */
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.SharedData = class LinearMovementByAngleSharedData {
  constructor(sharedData) {
    
  }
  
  // Shared properties:
  
}

gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.getSharedData = function(instanceContainer, behaviorName) {
  if (!instanceContainer._LinearMovement_LinearMovementByAngleSharedData) {
    const initialData = instanceContainer.getInitialSharedDataForBehavior(
      behaviorName
    );
    instanceContainer._LinearMovement_LinearMovementByAngleSharedData = new gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.SharedData(
      initialData
    );
  }
  return instanceContainer._LinearMovement_LinearMovementByAngleSharedData;
}

// Methods:
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.doStepPreEventsContext = {};
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.doStepPreEventsContext.idToCallbackMap = new Map();
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.doStepPreEventsContext.GDObjectObjects1= [];
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.doStepPreEventsContext.GDObjectObjects2= [];


gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.doStepPreEventsContext.eventsList0 = function(runtimeScene, eventsFunctionContext) {

{


let isConditionTrue_0 = false;
{
gdjs.copyArray(eventsFunctionContext.getObjects("Object"), gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.doStepPreEventsContext.GDObjectObjects1);
{for(var i = 0, len = gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.doStepPreEventsContext.GDObjectObjects1.length ;i < len;++i) {
    gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.doStepPreEventsContext.GDObjectObjects1[i].addPolarForce((gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.doStepPreEventsContext.GDObjectObjects1[i].getAngle()), eventsFunctionContext.getObjects("Object")[0].getBehavior(eventsFunctionContext.getBehaviorName("Behavior"))._getSpeed(), 0);
}
}
}

}


};

gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.doStepPreEvents = function(parentEventsFunctionContext) {
this._onceTriggers.startNewFrame();
var that = this;
var runtimeScene = this._runtimeScene;
let scopeInstanceContainer = null;
var thisObjectList = [this.owner];
var Object = Hashtable.newFrom({Object: thisObjectList});
var Behavior = this.name;
var eventsFunctionContext = {
  _objectsMap: {
"Object": Object
},
  _objectArraysMap: {
"Object": thisObjectList
},
  _behaviorNamesMap: {
"Behavior": Behavior
},
  globalVariablesForExtension: runtimeScene.getGame().getVariablesForExtension("LinearMovement"),
  sceneVariablesForExtension: runtimeScene.getScene().getVariablesForExtension("LinearMovement"),
  localVariables: [],
  getObjects: function(objectName) {
    return eventsFunctionContext._objectArraysMap[objectName] || [];
  },
  getObjectsLists: function(objectName) {
    return eventsFunctionContext._objectsMap[objectName] || null;
  },
  getBehaviorName: function(behaviorName) {
    return eventsFunctionContext._behaviorNamesMap[behaviorName] || behaviorName;
  },
  createObject: function(objectName) {
    const objectsList = eventsFunctionContext._objectsMap[objectName];
    if (objectsList) {
      const object = parentEventsFunctionContext && !(scopeInstanceContainer && scopeInstanceContainer.isObjectRegistered(objectName)) ?
        parentEventsFunctionContext.createObject(objectsList.firstKey()) :
        runtimeScene.createObject(objectsList.firstKey());
      if (object) {
        objectsList.get(objectsList.firstKey()).push(object);
        if (!(scopeInstanceContainer && scopeInstanceContainer.isObjectRegistered(objectName))) {
          eventsFunctionContext._objectArraysMap[objectName].push(object);
        }
      }
      return object;
    }
    return null;
  },
  getInstancesCountOnScene: function(objectName) {
    const objectsList = eventsFunctionContext._objectsMap[objectName];
    let count = 0;
    if (objectsList) {
      for(const objectName in objectsList.items)
        count += parentEventsFunctionContext && !(scopeInstanceContainer && scopeInstanceContainer.isObjectRegistered(objectName)) ?
parentEventsFunctionContext.getInstancesCountOnScene(objectName) :
        runtimeScene.getInstancesCountOnScene(objectName);
    }
    return count;
  },
  getLayer: function(layerName) {
    return runtimeScene.getLayer(layerName);
  },
  getArgument: function(argName) {
    return "";
  },
  getOnceTriggers: function() { return that._onceTriggers; }
};

gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.doStepPreEventsContext.GDObjectObjects1.length = 0;
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.doStepPreEventsContext.GDObjectObjects2.length = 0;

gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.doStepPreEventsContext.eventsList0(runtimeScene, eventsFunctionContext);
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.doStepPreEventsContext.GDObjectObjects1.length = 0;
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.doStepPreEventsContext.GDObjectObjects2.length = 0;


return;
}
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SpeedContext = {};
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SpeedContext.idToCallbackMap = new Map();
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SpeedContext.GDObjectObjects1= [];
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SpeedContext.GDObjectObjects2= [];


gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SpeedContext.eventsList0 = function(runtimeScene, eventsFunctionContext) {

{


let isConditionTrue_0 = false;
{
{eventsFunctionContext.returnValue = eventsFunctionContext.getObjects("Object")[0].getBehavior(eventsFunctionContext.getBehaviorName("Behavior"))._getSpeed();}
}

}


};

gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.Speed = function(parentEventsFunctionContext) {

var that = this;
var runtimeScene = this._runtimeScene;
let scopeInstanceContainer = null;
var thisObjectList = [this.owner];
var Object = Hashtable.newFrom({Object: thisObjectList});
var Behavior = this.name;
var eventsFunctionContext = {
  _objectsMap: {
"Object": Object
},
  _objectArraysMap: {
"Object": thisObjectList
},
  _behaviorNamesMap: {
"Behavior": Behavior
},
  globalVariablesForExtension: runtimeScene.getGame().getVariablesForExtension("LinearMovement"),
  sceneVariablesForExtension: runtimeScene.getScene().getVariablesForExtension("LinearMovement"),
  localVariables: [],
  getObjects: function(objectName) {
    return eventsFunctionContext._objectArraysMap[objectName] || [];
  },
  getObjectsLists: function(objectName) {
    return eventsFunctionContext._objectsMap[objectName] || null;
  },
  getBehaviorName: function(behaviorName) {
    return eventsFunctionContext._behaviorNamesMap[behaviorName] || behaviorName;
  },
  createObject: function(objectName) {
    const objectsList = eventsFunctionContext._objectsMap[objectName];
    if (objectsList) {
      const object = parentEventsFunctionContext && !(scopeInstanceContainer && scopeInstanceContainer.isObjectRegistered(objectName)) ?
        parentEventsFunctionContext.createObject(objectsList.firstKey()) :
        runtimeScene.createObject(objectsList.firstKey());
      if (object) {
        objectsList.get(objectsList.firstKey()).push(object);
        if (!(scopeInstanceContainer && scopeInstanceContainer.isObjectRegistered(objectName))) {
          eventsFunctionContext._objectArraysMap[objectName].push(object);
        }
      }
      return object;
    }
    return null;
  },
  getInstancesCountOnScene: function(objectName) {
    const objectsList = eventsFunctionContext._objectsMap[objectName];
    let count = 0;
    if (objectsList) {
      for(const objectName in objectsList.items)
        count += parentEventsFunctionContext && !(scopeInstanceContainer && scopeInstanceContainer.isObjectRegistered(objectName)) ?
parentEventsFunctionContext.getInstancesCountOnScene(objectName) :
        runtimeScene.getInstancesCountOnScene(objectName);
    }
    return count;
  },
  getLayer: function(layerName) {
    return runtimeScene.getLayer(layerName);
  },
  getArgument: function(argName) {
    return "";
  },
  getOnceTriggers: function() { return that._onceTriggers; }
};

gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SpeedContext.GDObjectObjects1.length = 0;
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SpeedContext.GDObjectObjects2.length = 0;

gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SpeedContext.eventsList0(runtimeScene, eventsFunctionContext);
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SpeedContext.GDObjectObjects1.length = 0;
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SpeedContext.GDObjectObjects2.length = 0;


return Number(eventsFunctionContext.returnValue) || 0;
}
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SetSpeedContext = {};
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SetSpeedContext.idToCallbackMap = new Map();
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SetSpeedContext.GDObjectObjects1= [];
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SetSpeedContext.GDObjectObjects2= [];


gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SetSpeedContext.eventsList0 = function(runtimeScene, eventsFunctionContext) {

{


let isConditionTrue_0 = false;
{
{eventsFunctionContext.getObjects("Object")[0].getBehavior(eventsFunctionContext.getBehaviorName("Behavior"))._setSpeed(eventsFunctionContext.getArgument("Value"))
}
}

}


};

gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SetSpeed = function(Value, parentEventsFunctionContext) {

var that = this;
var runtimeScene = this._runtimeScene;
let scopeInstanceContainer = null;
var thisObjectList = [this.owner];
var Object = Hashtable.newFrom({Object: thisObjectList});
var Behavior = this.name;
var eventsFunctionContext = {
  _objectsMap: {
"Object": Object
},
  _objectArraysMap: {
"Object": thisObjectList
},
  _behaviorNamesMap: {
"Behavior": Behavior
},
  globalVariablesForExtension: runtimeScene.getGame().getVariablesForExtension("LinearMovement"),
  sceneVariablesForExtension: runtimeScene.getScene().getVariablesForExtension("LinearMovement"),
  localVariables: [],
  getObjects: function(objectName) {
    return eventsFunctionContext._objectArraysMap[objectName] || [];
  },
  getObjectsLists: function(objectName) {
    return eventsFunctionContext._objectsMap[objectName] || null;
  },
  getBehaviorName: function(behaviorName) {
    return eventsFunctionContext._behaviorNamesMap[behaviorName] || behaviorName;
  },
  createObject: function(objectName) {
    const objectsList = eventsFunctionContext._objectsMap[objectName];
    if (objectsList) {
      const object = parentEventsFunctionContext && !(scopeInstanceContainer && scopeInstanceContainer.isObjectRegistered(objectName)) ?
        parentEventsFunctionContext.createObject(objectsList.firstKey()) :
        runtimeScene.createObject(objectsList.firstKey());
      if (object) {
        objectsList.get(objectsList.firstKey()).push(object);
        if (!(scopeInstanceContainer && scopeInstanceContainer.isObjectRegistered(objectName))) {
          eventsFunctionContext._objectArraysMap[objectName].push(object);
        }
      }
      return object;
    }
    return null;
  },
  getInstancesCountOnScene: function(objectName) {
    const objectsList = eventsFunctionContext._objectsMap[objectName];
    let count = 0;
    if (objectsList) {
      for(const objectName in objectsList.items)
        count += parentEventsFunctionContext && !(scopeInstanceContainer && scopeInstanceContainer.isObjectRegistered(objectName)) ?
parentEventsFunctionContext.getInstancesCountOnScene(objectName) :
        runtimeScene.getInstancesCountOnScene(objectName);
    }
    return count;
  },
  getLayer: function(layerName) {
    return runtimeScene.getLayer(layerName);
  },
  getArgument: function(argName) {
if (argName === "Value") return Value;
    return "";
  },
  getOnceTriggers: function() { return that._onceTriggers; }
};

gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SetSpeedContext.GDObjectObjects1.length = 0;
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SetSpeedContext.GDObjectObjects2.length = 0;

gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SetSpeedContext.eventsList0(runtimeScene, eventsFunctionContext);
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SetSpeedContext.GDObjectObjects1.length = 0;
gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle.prototype.SetSpeedContext.GDObjectObjects2.length = 0;


return;
}


gdjs.registerBehavior("LinearMovement::LinearMovementByAngle", gdjs.evtsExt__LinearMovement__LinearMovementByAngle.LinearMovementByAngle);
