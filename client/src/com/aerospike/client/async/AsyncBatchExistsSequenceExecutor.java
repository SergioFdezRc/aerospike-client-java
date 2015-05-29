/*
 * Copyright 2012-2015 Aerospike, Inc.
 *
 * Portions may be licensed to Aerospike, Inc. under one or more contributor
 * license agreements WHICH ARE COMPATIBLE WITH THE APACHE LICENSE, VERSION 2.0.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.aerospike.client.async;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.command.BatchNode;
import com.aerospike.client.command.BatchNode.BatchNamespace;
import com.aerospike.client.listener.ExistsSequenceListener;
import com.aerospike.client.policy.BatchPolicy;

public final class AsyncBatchExistsSequenceExecutor extends AsyncBatchExecutor {
	private final ExistsSequenceListener listener;

	public AsyncBatchExistsSequenceExecutor(
		AsyncCluster cluster,
		BatchPolicy policy, 
		Key[] keys,
		ExistsSequenceListener listener
	) {
		super(cluster, policy, keys);
		this.listener = listener;
		
		// Create commands.
		AsyncMultiCommand[] tasks = new AsyncMultiCommand[super.taskSize];
		int count = 0;

		for (BatchNode batchNode : batchNodes) {			
			if (batchNode.node.hasBatchIndex) {
				// New batch
				tasks[count++] = new AsyncBatchExistsSequence(this, cluster, batchNode, policy, keys, listener);
			}
			else {
				// Old batch only allows one namespace per call.
				for (BatchNamespace batchNamespace : batchNode.batchNamespaces) {
					tasks[count++] = new AsyncBatchExistsSequenceOld(this, cluster, (AsyncNode)batchNode.node, batchNamespace, policy, keys, listener);
				}
			}
		}
		// Dispatch commands to nodes.
		execute(tasks, policy.maxConcurrentThreads);
	}
	
	protected void onSuccess() {
		listener.onSuccess();
	}
	
	protected void onFailure(AerospikeException ae) {
		listener.onFailure(ae);
	}		
}
