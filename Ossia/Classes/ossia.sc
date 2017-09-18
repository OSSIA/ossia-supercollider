
	//-------------------------------------------//
	//                CUSTOM TYPES               //
	//-------------------------------------------//

OSSIA {

	*domain_list { |type ... args|
		^OSSIA_domain_list(type, args);
	}

	*domain_range { |min, max|
		^OSSIA_domain_range(min, max);
	}

	*access_mode { ^OSSIA_access_mode }
	*bounding_mode { ^OSSIA_bounding_mode }

	*vec2f { |v1 = 0.0, v2 = 0.0|
		^OSSIA_vec2f(v1, v2);
	}

	*vec3f { |v1 = 0.0, v2 = 0.0, v3 = 0.0|
		^OSSIA_vec3f(v1, v2, v3);
	}

	*vec4f { |v1 = 0.0, v2 = 0.0, v3 = 0.0, v4 = 0.0|
		^OSSIA_vec4f(v1, v2, v3, v4);
	}

}

OSSIA_vec2f[slot] : OSSIA_FVector
{
	*new {|v1 = 0.0, v2 = 0.0|
		^super.new(2, v1.asFloat, v2.asFloat);
	}
}

OSSIA_vec3f[slot] : OSSIA_FVector
{
	*new {|v1 = 0.0, v2 = 0.0, v3 = 0.0|
		^super.new(3, v1.asFloat, v2.asFloat, v3.asFloat);
	}
}

OSSIA_vec4f[slot] : OSSIA_FVector
{
	*new {|v1 = 0.0, v2 = 0.0, v3 = 0.0, v4 = 0.0|
		^super.new(4, v1.asFloat, v2.asFloat, v3.asFloat, v4.asFloat);
	}
}

OSSIA_FVector[slot] {

	var am_val, m_sz;

	*new {|sz ... values|
		^super.new.init(sz, values);
	}

	init { |sz,v|

		v.do({|item|
			if((item.isFloat.not) && (item.isInteger.not)) {
				Error("OSSIA: Error! Arguments are not of Float type").throw;
			};
		});

		am_val = v;
		m_sz = sz;
	}

	at {|i| ^am_val[i] }
}

OSSIA_domain_list[slot] {

	var m_values, m_type;

	*new { |type ... args|
		^super.new.init(type, args);
	}

	init { |type, args|

		m_values = args[0];
		m_type = type;
	}

	at {|i| ^m_values[i] }
}

OSSIA_domain_range[slot] {

	var m_values;

	*new { |min, max|
		^super.new.init(min,max);
	}

	init { |min, max|
		if(min > max) { Error("OSSIA: Error! check min/max values").throw; };
		m_values = Array.newClear(2);
		m_values[0] = min;
		m_values[1] = max;
	}

	at {|i|
		if(i <= 1) { ^m_values[i] };
	}

	min { ^m_values[0] }
	max { ^m_values[1] }

}

OSSIA_access_mode {
	*bi { ^'bi' }
	*get { ^'get' }
	*set { ^'set' }
}

OSSIA_bounding_mode {
	*free { ^'free' }
	*clip { ^'clip' }
	*low { ^'low' }
	*high { ^'high' }
	*wrap { ^'wrap' }
	*fold { ^'fold' }
}

	//-------------------------------------------//
	//                   NODES                   //
	//-------------------------------------------//

OSSIA_Node {

	var <m_node_id;

	*new { |parent, name|
		^super.new.nodeCtor(parent, name);
	}

	*newFromChild {
		^super.new
	}

	nodeCtor { |parent, name|
		_OSSIA_InstantiateNode
		^this.primitiveFailed
	}

	name {
		_OSSIA_NodeGetName
		^this.primitiveFailed
	}

	fullpath {
		_OSSIA_NodeGetFullPath
		^this.primitiveFailed
	}

	children {
		_OSSIA_NodeGetChildrenNames
		^this.primitiveFailed
	}

	disabled {
		_OSSIA_NodeGetDisabled
		^this.primitiveFailed
	}

	disabled_ { |aBool|
		_OSSIA_NodeSetDisabled
		^this.primitiveFailed
	}

	hidden {
		_OSSIA_NodeGetHidden
		^this.primitiveFailed
	}

	hidden_ { |aBool|
		_OSSIA_NodeSetHidden
		^this.primitiveFailed
	}

	muted {
		_OSSIA_NodeGetMuted
		^this.primitiveFailed
	}

	muted_ { |aBool|
		_OSSIA_NodeSetMuted
		^this.primitiveFailed
	}

	description {
		_OSSIA_NodeGetDescription
		^this.primitiveFailed
	}

	description_ { |aString|
		_OSSIA_NodeSetDescription
		^this.primitiveFailed
	}

	tags {
		_OSSIA_NodeGetTags
		^this.primitiveFailed
	}

	tags_ { |aSymbolList|
		_OSSIA_NodeSetTags
		^this.primitiveFailed
	}

	zombie {
		_OSSIA_NodeGetZombie
		^this.primitiveFailed
	}

	load_preset { |path| // TODO: force the .json extension
		path !? { this.pyrPresetLoad(path) };
		path ?? { Dialog.openPanel({|p| this.pyrPresetLoad(p)}); };
	}

	save_preset { |path|
		path !? { this.pyrPresetSave(path) };
		path ?? { Dialog.savePanel({|p| this.pyrPresetSave(p)}); };
	}

	pyrPresetLoad { |path|
		_OSSIA_PresetLoad
		^this.primitiveFailed
	}

	pyrPresetSave { |path|
		_OSSIA_PresetSave
		^this.primitiveFailed
	}

}

OSSIA_Device : OSSIA_Node {

	classvar count;
	var m_semaphore;

	*new {|name|

		count !? { count = count + 1 };
		count ?? {
			count = 1;
			ShutDown.add({OSSIA_Device.pyrShutdown()});
		};

		^super.newFromChild.pyrDeviceCtor(name);
	}

	//-------------------------------------------//
	//               DEVICE CALLBACKS            //
	//-------------------------------------------//

	forkExpose { |method, vargs, callback|

		callback !? {
			m_semaphore = Semaphore(1);
			fork {
				m_semaphore.wait();
				this.exposeRedirect(method, vargs);
			};

			fork {
				if(callback.isKindOf(Function)) {
					callback.value();
				}
			};
		};

		callback ?? {
			this.exposeRedirect(method, vargs);
		};
	}

	exposeRedirect { |method, vargs|
		switch(method,
			'oscqs', { this.pyrOSCQS(vargs[0], vargs[1])},
			'oscqm', { this.pyrOSCQM(vargs[0])},
			'minuit', { this.pyrMinuit(vargs[0], vargs[1], vargs[2])},
			'osc', { this.pyrOSC(vargs[0], vargs[1], vargs[2])}
		);
	}

	//-------------------------------------------//
	//                NEW SHORTCUTS              //
	//-------------------------------------------//

	*newOSCQueryServer { |name, osc_port = 1234, ws_port = 5678, callback|
		^this.new(name).exposeOSCQueryServer(osc_port, ws_port, callback);
	}

	*newOSCQueryMirror { |name, host_addr, callback|
		^this.new(name).exposeOSCQueryMirror(host_addr, callback);
	}

	*newMinuit { |name, remote_ip, remote_port, local_port, callback|
		^this.new(name).exposeMinuit(remote_ip, remote_port, local_port, callback);
	}

	*newOSC { |name, remote_ip, remote_port, local_port, callback|
		^this.new(name).exposeOSC(remote_ip, remote_port, local_port, callback);
	}

	//-------------------------------------------//
	//                   EXPOSE                  //
	//-------------------------------------------//

	exposeOSCQueryServer { |osc_port = 1234, ws_port = 5678, callback|
		this.forkExpose('oscqs', [osc_port, ws_port], callback);
	}

	exposeOSCQueryMirror { |host_addr, callback|
		this.forkExpose('oscqm', [host_addr], callback);
	}

	exposeMinuit { |remote_ip, remote_port, local_port, callback|
		this.forkExpose('minuit', [remote_ip, remote_port, local_port], callback);
	}

	exposeOSC { |remote_ip, remote_port, local_port, callback|
		this.forkExpose('osc', [remote_ip, remote_port, local_port], callback);
	}

	//-------------------------------------------//
	//             PRIMITIVE CALLS               //
	//-------------------------------------------//

	pyrOSCQS { |osc_port, ws_port|
		_OSSIA_ExposeOSCQueryServer
		^this.primitiveFailed
	}

	pyrOSCQM { |host_addr|
		_OSSIA_ExposeOSCQueryMirror
		^this.primitiveFailed;
	}

	pyrMinuit { |remote_ip, remote_port, local_port|
		_OSSIA_ExposeMinuit
		^this.primitiveFailed;
	}

	pyrOSC { |remote_ip, remote_port, local_port|
		_OSSIA_ExposeOSC
		^this.primitiveFailed
	}

	pyrDeviceCtor { |name|
		_OSSIA_InstantiateDevice
		^this.primitiveFailed
	}

	free {
		_OSSIA_FreeDevice
		^this.primitiveFailed;
	}

	*pyrShutdown {
		_OSSIA_Dtor
		^this.primitiveFailed
	}

}

OSSIA_Parameter : OSSIA_Node {

	var m_callback, m_has_callback;

	*new {|parent_node, name, type, domain, default_value,
		bounding_mode = 'free', critical = false,
		repetition_filter = false|

		^super.newFromChild.parameterCtor(parent_node, name, type, domain, default_value,
			                bounding_mode, critical, repetition_filter);
	}

	parameterCtor { |parent, name, type, domain, default_value,
		bounding_mode, critical, repetition_filter|

		m_has_callback = false;

		this.pyrCtor(parent, name, type, domain, default_value,
			bounding_mode, critical, repetition_filter);
	}

	pyrCtor { |parent_name, node_name, type, domain, default_value,
		bounding_mode, critical, repetition_filter|
		_OSSIA_InstantiateParameter
		^this.primitiveFailed
	}

	//-------------------------------------------//
	//                PROPERTIES                 //
	//-------------------------------------------//

	value {
		_OSSIA_ParameterGetValue
		^this.primitiveFailed
	}

	value_ { |value|
		if(m_has_callback)
		{
			m_callback.value(value);
		};

		this.pyrSetValue(value);
	}

	pyrSetValue { |v|
		_OSSIA_ParameterSetValue
		^this.primitiveFailed
	}

	access_mode {
		_OSSIA_ParameterGetAccessMode
		^this.primitiveFailed
	}

	access_mode_ { |aSymbol|
		_OSSIA_ParameterSetAccessMode
		^this.primitiveFailed
	}

	domain {
		_OSSIA_ParameterGetDomain
		^this.primitiveFailed
	}

	domain_ { |aList|
		_OSSIA_ParameterSetDomain
		^this.primitiveFailed
	}

	bounding_mode {
		_OSSIA_ParameterGetBoundingMode
		^this.primitiveFailed
	}

	bounding_mode_ { |aSymbol|
		_OSSIA_ParameterSetBoundingMode
		^this.primitiveFailed
	}

	repetition_filter {
		_OSSIA_ParameterGetRepetitionFilter
		^this.primitiveFailed
	}

	repetition_filter_ { |aBool|
		_OSSIA_ParameterSetRepetitionFilter
		^this.primitiveFailed
	}

	unit {
		_OSSIA_ParameterGetUnit
		^this.primitiveFailed
	}

	unit_ { |aSymbol|
		_OSSIA_ParameterSetUnit
		^this.primitiveFailed
	}

	priority {
		_OSSIA_ParameterGetPriority
		^this.primitiveFailed
	}

	priority_ { |aFloat|
		_OSSIA_ParameterSetPriority
		^this.primitiveFailed
	}

	critical {
		_OSSIA_ParameterGetCritical
		^this.primitiveFailed
	}

	critical_ { |aBool|
		_OSSIA_ParameterSetCritical
		^this.primitiveFailed
	}

	//-------------------------------------------//
	//                 CALLBACKS                 //
	//-------------------------------------------//

	callback { ^m_callback }
	callback_ { |callback_function|
		if(not(m_has_callback))
		{
			this.prEnableCallback();
			m_has_callback = true;
		}
		{
			if(callback_function.isNil())
			{
				this.prDisableCallback;
				m_has_callback = false;
			}
		};

		m_callback = callback_function;
	}

	prEnableCallback
	{
		_OSSIA_ParameterSetCallback
		^this.primitiveFailed
	}

	prDisableCallback
	{
		_OSSIA_ParameterRemoveCallback
		^this.primitiveFailed
	}

	// interpreter callback from attached ossia lambda
	pvOnCallback { |v|
		m_callback.value(v);
	}

	//-------------------------------------------//
	//            SHORTCUTS & ALIASES            //
	//-------------------------------------------//

	v { ^this.value() }
	v_ { |value| this.value_(value) }
	sv { |value| this.value_(value) }

	// CONVENIENCE DEF MTHODS (change to kr, ar etc.) --> this will be in mgu

	sym { ^(this.name ++ "_" ++ m_node_id).asSymbol }
	aar { ^[this.sym, this.value()]; }


	kr { | bind = true |

		if(bind) {
			if(not(m_has_callback)) { this.prEnableCallback; m_has_callback = true };
			m_callback = { |v| Server.default.sendMsg("/n_set", 0, this.sym, v) };
		}

		^this.sym.kr
	}

	ar { ^this.sym.ar}
	tr { ^this.sym.tr}

}

OSSIA_MirrorParameter : OSSIA_Parameter {
	*new { |device, address|
		^super.newFromChild.mirrorInit.pyrGetMirror(device, address);
	}

	mirrorInit { m_has_callback = false }

	pyrGetMirror { |device, addr|
		_OSSIA_NodeGetMirror
		^this.primitiveFailed
	}
}

// basically the same thing, but with different inheritance

OSSIA_MirrorNode : OSSIA_Node {
	*new { |device, address|
		^this.new.pyrGetMirror(device, address)
	}

	pyrGetMirror { |device, addr|
		_OSSIA_NodeGetMirror
		^this.primitiveFailed
	}
}

	//-------------------------------------------//
	//                   UNITS                   //
	//-------------------------------------------//

OSSIA_Unit {
	*position{ ^OSSIA_position }
	*orientation{^OSSIA_orientation }
	*color{^OSSIA_color}
	*angle{^OSSIA_angle}
	*distance{^OSSIA_distance}
	*time{^OSSIA_time}
	*gain{^OSSIA_gain}
	*speed{^OSSIA_speed}

	*fmt { |method|
		var separr = method.cs.drop(11).split($.);
		var res = separr[0] ++ "." ++ separr[1].split($')[1];
		^res;
	}
}

OSSIA_position : OSSIA_Unit {
	*cart2D{ ^this.fmt(thisMethod) }
	*cart3D{ ^this.fmt(thisMethod)}
	*spherical{ ^this.fmt(thisMethod) }
	*polar{ ^this.fmt(thisMethod) }
	*opengl{^this.fmt(thisMethod) }
	*cylindrical{^this.fmt(thisMethod) }
}

OSSIA_orientation : OSSIA_Unit {
	*quaternion{^this.fmt(thisMethod) }
	*euler{^this.fmt(thisMethod) }
	*axis{^this.fmt(thisMethod) }
}

OSSIA_color : OSSIA_Unit {
	*argb{^this.fmt(thisMethod) }
	*rgba{^this.fmt(thisMethod) }
	*rgb{^this.fmt(thisMethod) }
	*bgr{^this.fmt(thisMethod) }
	*argb8{^this.fmt(thisMethod) }
	*hsv{^this.fmt(thisMethod) }
	*cmy8{^this.fmt(thisMethod) }
	*css{^this.fmt(thisMethod) } // TODO
}

OSSIA_angle : OSSIA_Unit {
	*degree{^this.fmt(thisMethod) }
	*radian{^this.fmt(thisMethod) }
}

OSSIA_distance : OSSIA_Unit {
	*meter{^this.fmt(thisMethod) }
	*kilometer{^this.fmt(thisMethod) }
	*decimeter{^this.fmt(thisMethod) }
	*centimeter{^this.fmt(thisMethod) }
	*millimeter{^this.fmt(thisMethod) }
	*micrometer{^this.fmt(thisMethod) }
	*nanometer{^this.fmt(thisMethod) }
	*picometer{^this.fmt(thisMethod) }
	*inch{^this.fmt(thisMethod) }
	*foot{^this.fmt(thisMethod) }
	*mile{^this.fmt(thisMethod) }
}

OSSIA_time : OSSIA_Unit {
	*second{^this.fmt(thisMethod) }
	*bark{^this.fmt(thisMethod) }
	*bpm{^this.fmt(thisMethod) }
	*cent{^this.fmt(thisMethod) }
	*frequency{^this.fmt(thisMethod) }
	*mel{^this.fmt(thisMethod) }
	*midi_pitch{^this.fmt(thisMethod) }
	*millisecond{^this.fmt(thisMethod) }
	*playback_speed{^this.fmt(thisMethod) }
}

OSSIA_gain : OSSIA_Unit {
	*linear{^this.fmt(thisMethod) }
	*midigain{^this.fmt(thisMethod) }
	*decibel{^this.fmt(thisMethod) }
	*decibel_raw{^this.fmt(thisMethod) }
}

OSSIA_speed : OSSIA_Unit {
	*meter_per_second{^this.fmt(thisMethod) }
	*miles_per_hour{^this.fmt(thisMethod) }
	*kilometer_per_hour{^this.fmt(thisMethod) }
	*knot{^this.fmt(thisMethod) }
	*foot_per_second{^this.fmt(thisMethod) }
	*foot_per_hour{^this.fmt(thisMethod) }
}