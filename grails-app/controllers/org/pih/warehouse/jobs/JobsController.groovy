/**
 * Copyright (c) 2012 Partners In Health.  All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 **/
package org.pih.warehouse.jobs

import grails.gorm.transactions.Transactional
import grails.plugins.quartz.GrailsJobClassConstants
import grails.plugins.quartz.JobDescriptor
import grails.plugins.quartz.JobManagerService
import org.quartz.JobDetail
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.Trigger
import org.quartz.TriggerKey
import org.quartz.core.QuartzScheduler
import org.quartz.impl.StdScheduler
import org.quartz.impl.matchers.GroupMatcher

import java.text.ParseException

@Transactional
class JobsController {

    JobManagerService jobManagerService

    Scheduler getQuartzScheduler() {
        return jobManagerService.quartzScheduler
    }

    def index() {
        redirect(action: "list", params: params)
    }

    def list() {
        Set<JobKey> jobKeys = []
        quartzScheduler.jobGroupNames.each { String groupName ->
            jobKeys.addAll(quartzScheduler.getJobKeys(GroupMatcher.groupEquals(groupName)))
        }

        [jobKeys: jobKeys]
    }

    def show() {
        String jobGroup = params.group ?: GrailsJobClassConstants.DEFAULT_GROUP
        JobKey jobKey = new JobKey(params.id, jobGroup)
        JobDetail jobDetail = quartzScheduler.getJobDetail(jobKey)
        JobDescriptor jobDescriptor = (jobDetail) ? JobDescriptor.build(jobDetail, quartzScheduler) : null
        if (!jobDescriptor) {
            throw new SchedulerException("No Job Detail for key ${params.id}")
        }
        def triggers = quartzScheduler.getTriggersOfJob(jobKey)
        log.info "triggers " + triggers

        [jobDescriptor: jobDescriptor, jobDetail: jobDetail, jobKey: jobKey, triggers: triggers]
    }


    def unscheduleJob() {
        // find jobKey of job
        JobKey jobKey = JobKey.jobKey(params.id)
        if (jobKey) {
            JobDetail jobDetail = quartzScheduler.getJobDetail(jobKey)

            // get list of existing triggers
            def triggersList = quartzScheduler.getTriggersOfJob(jobKey)
            triggersList.each {
                log.info "Unscheduling trigger " + it
                // remove all existing triggers
                quartzScheduler.unscheduleJob(it.key)
            }
        } else {
            flash.message = "Unable to find job with jobKey = ${params.id}"
        }
        redirect(action: "show", id: params.id)
    }

    def unscheduleTrigger() {
        // find jobKey of job

        TriggerKey triggerKey = TriggerKey.triggerKey(params.id)
        Trigger trigger = quartzScheduler.getTrigger(triggerKey)
        JobKey jobKey = trigger.jobKey
        if (trigger) {
            quartzScheduler.unscheduleJob(triggerKey)
        } else {
            flash.message = "Unable to unschedule trigger with trigger key ${params.id}"
        }
        redirect(action: "show", id: jobKey.name)
    }

    def scheduleJob() {
        JobKey jobKey = JobKey.jobKey(params.id)
        if (jobKey) {
            // cronExpression 0 0 22 * * ?
            try {
                Trigger trigger //= TriggerHelper.cronTrigger(jobKey, params.cronExpression, [:])
                def date = quartzScheduler.scheduleJob(trigger)
                flash.message = "Job ${jobKey} scheduled " + date
            } catch (ParseException e) {
                flash.message = "Unable to schedule job with cron expression ${params.cronExpression} due to the following error: " + e.message
            }
        } else {
            flash.message = "Unable to find job with jobKey = ${params.id}"
        }

        redirect(action: "show", id: params.id)

    }

}
