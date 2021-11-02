use super::*;
use cctp_primitives::proving_system::*;
fn get_proving_system_type(_env: &JNIEnv, _proving_system: JObject) -> ProvingSystem {
    // Extract proving system type
    let proving_system = _env
        .call_method(_proving_system, "ordinal", "()I", &[])
        .expect("Should be able to call ordinal() on ProvingSystem enum")
        .i()
        .unwrap() as usize;

    // Convert to Rust enum
    match proving_system {
        0 => ProvingSystem::Undefined,
        1 => ProvingSystem::Darlin,
        2 => ProvingSystem::CoboundaryMarlin,
        _ => unreachable!(),
    }
}

ffi_export!(
    fn Java_com_horizen_provingsystemnative_ProvingSystem_nativeGenerateDLogKeys(
        _env: JNIEnv,
        _class: JClass,
        _proving_system: JObject,
        _max_segment_size: jint,
        _supported_segment_size: jint,
    ) {
        // Get proving system type
        let proving_system = get_proving_system_type(&_env, _proving_system);

        // Generate DLOG keypair
        ok_or_throw_exc!(
            _env,
            init_dlog_keys(
                proving_system,
                _max_segment_size as usize,
                _supported_segment_size as usize,
            ),
            "com/horizen/provingsystemnative/ProvingSystemException",
            "Unable to initialize DLOG keys"
        )
    }
);

ffi_export!(
    fn Java_com_horizen_provingsystemnative_ProvingSystem_nativeCheckProofVkSize(
        _env: JNIEnv,
        _class: JClass,
        _zk: jboolean,
        _supported_segment_size: jint,
        _max_proof_size: jint,
        _max_vk_size: jint,
        _verification_key_path: JString,
    ) -> jboolean {
        // Read vk from file

        //Extract vk path
        let vk_path = _env
            .get_string(_verification_key_path)
            .expect("Should be able to read jstring as Rust String");

        // Deserialize vk
        let vk = ok_or_throw_exc!(
            &_env,
            read_from_file::<ZendooVerifierKey>(vk_path.to_str().unwrap(), Some(false), Some(true)),
            "com/horizen/provingsystemnative/ProvingSystemException",
            "Unable to read vk from file",
            JNI_FALSE
        );

        // Read zk value
        let zk = _zk == JNI_TRUE;

        // Get ps type from vk
        let ps_type = vk.get_proving_system_type();

        // Get index info from vk
        let index_info = match vk {
            ZendooVerifierKey::CoboundaryMarlin(cob_marlin_vk) => cob_marlin_vk.index_info,
            ZendooVerifierKey::Darlin(darlin_vk) => darlin_vk.index_info,
        };

        // Perform check
        let result = check_proof_vk_size(
            _supported_segment_size as usize,
            index_info,
            zk,
            ps_type,
            _max_proof_size as usize,
            _max_vk_size as usize,
        );

        if result {
            JNI_TRUE
        } else {
            JNI_FALSE
        }
    }
);

fn get_proving_system_type_as_jint(_env: &JNIEnv, ps: ProvingSystem) -> jint {
    match ps {
        ProvingSystem::Undefined => 0i32 as jint,
        ProvingSystem::Darlin => 1i32 as jint,
        ProvingSystem::CoboundaryMarlin => 2i32 as jint,
    }
}

ffi_export!(
    fn Java_com_horizen_provingsystemnative_ProvingSystem_nativeGetProverKeyProvingSystemType(
        _env: JNIEnv,
        _class: JClass,
        _proving_key_path: JString,
    ) -> jint {
        // Read paths
        let proving_key_path = _env
            .get_string(_proving_key_path)
            .expect("Should be able to read jstring as Rust String");

        let ps = ok_or_throw_exc!(
            &_env,
            read_from_file::<ProvingSystem>(proving_key_path.to_str().unwrap(), None, None),
            "com/horizen/provingsystemnative/ProvingSystemException",
            "Unable to read ProvingSystem type from file",
            -1i32 as jint
        );

        get_proving_system_type_as_jint(&_env, ps)
    }
);

ffi_export!(
    fn Java_com_horizen_provingsystemnative_ProvingSystem_nativeGetVerifierKeyProvingSystemType(
        _env: JNIEnv,
        _class: JClass,
        _verifier_key_path: JString,
    ) -> jint {
        // Read paths
        let verifier_key_path = _env
            .get_string(_verifier_key_path)
            .expect("Should be able to read jstring as Rust String");

        let ps = ok_or_throw_exc!(
            &_env,
            read_from_file::<ProvingSystem>(verifier_key_path.to_str().unwrap(), None, None),
            "com/horizen/provingsystemnative/ProvingSystemException",
            "Unable to read ProvingSystem type from file",
            -1i32 as jint
        );

        get_proving_system_type_as_jint(&_env, ps)
    }
);

ffi_export!(
    fn Java_com_horizen_provingsystemnative_ProvingSystem_nativeGetProofProvingSystemType(
        _env: JNIEnv,
        _class: JClass,
        _proof: jbyteArray,
    ) -> jint {
        //Extract proof
        let proof_bytes = _env
            .convert_byte_array(_proof)
            .expect("Should be able to convert to Rust byte array");

        match deserialize_from_buffer::<ProvingSystem>(&proof_bytes[..1], None, None) {
            Ok(ps) => get_proving_system_type_as_jint(&_env, ps),
            Err(_) => -1i32 as jint,
        }
    }
);