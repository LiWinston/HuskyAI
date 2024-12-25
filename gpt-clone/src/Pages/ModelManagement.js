// ModelManagement.js
import React, {useEffect, useState} from 'react';
import axios from 'axios';
import './admin.css';
import '../Component/sweetAlertUtil';
import {showSweetAlertWithRetVal} from "../Component/sweetAlertUtil";

function ModelManagement() {
    const [models, setModels] = useState([]);
    const [newModel, setNewModel] = useState(
        {url: '', name: '', apiKey: '', allowedModels: []});
    const [operationResult, setOperationResult] = useState(null);

// Retrieve the current list of models.
    const fetchModels = async () => {
        try {
            const response = await axios.get('/api/admin/models');
            setModels(response.data.data.modelServices);  // Adapt to the new VO structure.
        } catch (error) {
            console.error('Error fetching models:', error);
        }
    };

    useEffect(() => {
        fetchModels().then(r => console.log(r));
    }, []);

    // Dynamic registration model.
    const registerModel = async () => {
        try {
            const response = await axios.post('/api/admin/models', newModel);
            if (response.data.code !== 0) {
                setOperationResult('Model registered successfully.');
                showSweetAlertWithRetVal('Model registered successfully.', {icon: 'success', title: 'Success'}).then(async () => {
                    await fetchModels();  // Refresh the list after registering.
                })

            } else {
                setOperationResult('Failed to register model.');
                await showSweetAlertWithRetVal('Failed to register model.', {icon: 'error', title: 'Error'});
            }
        } catch (error) {
            console.error('Error registering model:', error);
            await showSweetAlertWithRetVal('Error registering model: ' + error.message, {icon: 'error', title: 'Error'});
        }
    };
    // Update model information.
    const updateModel = async (name, model, operation) => {
        try {
            await axios.post('/api/admin/models', {name, model, operation});
            await fetchModels();
        } catch (error) {
            console.error('Error updating model:', error);
        }
    };

    // Manually refresh the model.
    const refreshModels = async () => {
        try {
            await axios.post('/api/admin/models/refresh').then(res => {
                showSweetAlertWithRetVal("Added " + res.data.data.add + " models, removed " + res.data.data.remove + " models."
                    + (res.data.data.add + res.data.data.remove > 0 ? " Refreshed Log: " + res.data.msg : " No changes.")
                    , {
                        icon: res.data.code ? 'success' : 'error',
                        tittle: res.data.code ? 'Success' : 'Error'
                    }).then(pressRes => {
                    if (pressRes.isConfirmed) {
                        if (res.data.data.add + res.data.data.remove > 0) {
                            fetchModels();
                        }
                    }
                });
            })

        } catch
            (error) {
            console.error('Error refreshing models:', error);
        }
    }

    // Enable/disable all models.
    const toggleMultiModels = async (name, models, operation) => {
    try {
        const modelIds = models.map((m) => m.model);
        const response = await axios.post('/api/admin/models/manage', {
            name,
            model: modelIds,
            operation: operation
        });
        if (response.data.code === 1) {
            await showSweetAlertWithRetVal('Operation successful.', {icon: 'success', title: 'Success'});
        } else {
            await showSweetAlertWithRetVal('Operation failed: ' + response.data.msg, {icon: 'error', title: 'Error'});
        }
        await fetchModels();
    } catch (error) {
        console.error('Error disabling all models:', error);
        await showSweetAlertWithRetVal('Error: ' + error.message, {icon: 'error', title: 'Error'});
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
                    onChange={(e) => setNewModel(
                        {...newModel, apiKey: e.target.value})}
                />
                <input
                    type="text"
                    className="model-input"
                    placeholder="Allowed Models"
                    value={newModel.allowedModels}
                    onChange={(e) => setNewModel(
                        {...newModel, allowedModels: e.target.value.split(',')})}
                />
                <button className="ModelManagementButton"
                        onClick={registerModel}>Register Model
                </button>
            </div>

            <h4>Manage Models</h4>
            <ul className="model-list">
                {models.map((modelService) => (
                    <li key={modelService.url} className="model-item">
                        <div className="model-header">
                            <h5 className="model-title">{modelService.name ||
                                'Unnamed'} | {modelService.url}</h5>
                            <button
                                className="toggle-all-button"
                                onClick={() => toggleMultiModels(modelService.name,
                                    modelService.mdList,
                                    modelService.mdList.some(md => md.availableFromServer)
                                        ? 'DISABLE'
                                        : 'ENABLE')}
                            >
                                {modelService.mdList.some(md => md.availableFromServer)
                                    ? 'Disable All Models'
                                    : 'Enable All Models'}
                            </button>
                        </div>
                        <ul className="model-details">
                            {modelService.mdList.map((md) => (
                                <li key={md.model}>
                                    <div className="model-info">
                                        <span className="model-name">{md.model}</span>
                                        <label className="checkbox-label">
                                            <input
                                                type="checkbox"
                                                checked={md.allowed}
                                                onChange={() => updateModel(modelService.name,
                                                    md.model,
                                                    md.allowed ? 'NOTALLOW' : 'ALLOW')}
                                            />
                                            Allowed
                                        </label>
                                        <label className="checkbox-label">
                                            <input
                                                type="checkbox"
                                                checked={md.availableFromServer}
                                                onChange={() => updateModel(modelService.name,
                                                    md.model, md.availableFromServer
                                                        ? 'DISABLE'
                                                        : 'ENABLE')}
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
            <button className="ModelManagementButton"
                    onClick={refreshModels}>Refresh Models
            </button>
            {operationResult &&
                <div className="operation-result">{operationResult}</div>}
        </div>
    );
}

export default ModelManagement;
