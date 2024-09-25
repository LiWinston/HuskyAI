// ModelManagement.js
import React, {useEffect, useState} from 'react';
import axios from 'axios';
import './admin.css';

function ModelManagement() {
    const [models, setModels] = useState([]);
    const [newModel, setNewModel] = useState({url: '', name: '', apiKey: '', allowedModels: []});
    const [operationResult, setOperationResult] = useState(null);

// 获取当前的模型列表
    const fetchModels = async () => {
        try {
            const response = await axios.get(window.API_BASE_URL + '/admin/models');
            setModels(response.data.data.modelServices);  // 适配新的VO结构
        } catch (error) {
            console.error("Error fetching models:", error);
        }
    };

    useEffect(() => {
        fetchModels().then(r => console.log(r));
    }, []);

    // 动态注册模型
    const registerModel = async () => {
        try {
            const response = await axios.post('/admin/models', newModel);
            if (response.data.success) {
                setOperationResult("Model registered successfully.");
                await fetchModels();  // 注册后刷新列表
            } else {
                setOperationResult("Failed to register model.");
            }
        } catch (error) {
            console.error("Error registering model:", error);
        }
    };

    // 更新模型信息
    const updateModel = async (name, model, operation) => {
        try {
            await axios.post('/admin/models', {name, model, operation});
            await fetchModels();
        } catch (error) {
            console.error("Error updating model:", error);
        }
    };

    // 手动刷新模型
    const refreshModels = async () => {
        try {
            await axios.post('/admin/models/refresh');
            await fetchModels();
        } catch (error) {
            console.error("Error refreshing models:", error);
        }
    };

    // 启用/禁用所有模型
    const toggleMultiModels = async (name, models, operation) => {
        try {
            const modelIds = models.map((m) => m.model);
            await axios.post('/admin/models', {name, model: modelIds, operation: operation});
            await fetchModels();
        } catch (error) {
            console.error("Error disabling all models:", error);
        }
    };

    return (
        <div className="admin-interface">
            <h3>Admin Dashboard</h3>

            <h4>Model Management</h4>
            <h4>Register New Model</h4>
            <div className="model-registration">
                <input
                    type="text"
                    className="model-input"
                    placeholder="Model URL"
                    value={newModel.url}
                    onChange={(e) => setNewModel({...newModel, url: e.target.value})}
                />
                <input
                    type="text"
                    className="model-input"
                    placeholder="Model Name"
                    value={newModel.name}
                    onChange={(e) => setNewModel({...newModel, name: e.target.value})}
                />
                <input
                    type="text"
                    className="model-input"
                    placeholder="API Key (optional)"
                    value={newModel.apiKey}
                    onChange={(e) => setNewModel({...newModel, apiKey: e.target.value})}
                />
                <input
                    type="text"
                    className="model-input"
                    placeholder="Allowed Models"
                    value={newModel.allowedModels}
                    onChange={(e) => setNewModel({...newModel, allowedModels: e.target.value.split(',')})}
                />
                <button className="model-button" onClick={registerModel}>Register Model</button>
            </div>

            <h4>Manage Models</h4>
            <ul className="model-list">
                {models.map((modelService) => (
                    <li key={modelService.url} className="model-item">
                        <div>
                            <h5 className="model-title">{modelService.name || "Unnamed"} | {modelService.url}</h5>
                            <button
                                className="model-button"
                                onClick={() => toggleMultiModels(modelService.name, modelService.mdList, modelService.mdList.some(md => md.availableFromServer) ? 'DISABLE' : 'ENABLE')}
                            >
                                {modelService.mdList.some(md => md.availableFromServer) ? 'Disable All Models' : 'Enable All Models'}
                            </button>
                        </div>
                        <ul className="model-details">
                            {modelService.mdList.map((md) => (
                                <li key={md.model}>
                                    <div>
                                        <strong>Model:</strong> {md.model}
                                        <label>
                                            <input
                                                type="checkbox"
                                                checked={md.allowed}
                                                onChange={() => updateModel(modelService.name, md.model, md.allowed ? 'NOTALLOW' : 'ALLOW')}
                                            />
                                            Allowed
                                        </label>
                                        <label>
                                            <input
                                                type="checkbox"
                                                checked={md.availableFromServer}
                                                onChange={() => updateModel(modelService.name, md.model, md.availableFromServer ? 'DISABLE' : 'ENABLE')}
                                            />
                                            Available from Server
                                        </label>
                                    </div>
                                </li>
                            ))}
                        </ul>
                    </li>
                ))}
            </ul>

            <h4>Refresh Models</h4>
            <button className="model-button" onClick={refreshModels}>Refresh Models</button>
            {operationResult && <div className="operation-result">{operationResult}</div>}
        </div>
    );
}

export default ModelManagement;
